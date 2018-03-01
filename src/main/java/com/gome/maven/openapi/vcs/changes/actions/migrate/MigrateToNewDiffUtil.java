package com.gome.maven.openapi.vcs.changes.actions.migrate;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.DiffManager;
import com.gome.maven.openapi.diff.DiffNavigationContext;
import com.gome.maven.openapi.diff.DiffTool;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.diff.actions.impl.GoToChangePopupBuilder;
import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.diff.chains.DiffRequestProducer;
import com.gome.maven.diff.chains.DiffRequestProducerException;
import com.gome.maven.diff.chains.SimpleDiffRequestChain;
import com.gome.maven.diff.contents.BinaryFileContentImpl;
import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.contents.DocumentContentImpl;
import com.gome.maven.diff.contents.EmptyContent;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.ErrorDiffRequest;
import com.gome.maven.diff.requests.SimpleDiffRequest;
import com.gome.maven.diff.util.DiffUserDataKeys;
import com.gome.maven.diff.util.DiffUserDataKeysEx;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangeRequestChain;
import com.gome.maven.openapi.vcs.changes.actions.ChangeDiffRequestPresentable;
import com.gome.maven.openapi.vcs.changes.actions.DiffRequestPresentable;
import com.gome.maven.openapi.vcs.changes.actions.DiffRequestPresentableProxy;
import com.gome.maven.openapi.vcs.changes.actions.diff.ChangeGoToChangePopupAction;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.gome.maven.openapi.util.text.StringUtil.notNullize;

public class MigrateToNewDiffUtil {
    private static final Logger LOG = Logger.getInstance(MigrateToNewDiffUtil.class);

     public static final Object DO_NOT_TRY_MIGRATE = "doNotTryMigrate";

    
    public static DiffRequestChain convertRequestChain( com.gome.maven.openapi.diff.DiffRequest oldRequest) {
        ChangeRequestChain oldChain = (ChangeRequestChain)oldRequest.getGenericData().get(VcsDataKeys.DIFF_REQUEST_CHAIN.getName());
        if (oldChain == null || oldChain.getAllRequests().size() < 2) {
            DiffRequest request = convertRequest(oldRequest);
            return new SimpleDiffRequestChain(request);
        }
        else {
            return new ChangeRequestChainWrapper(oldChain);
        }
    }

    
    public static DiffRequest convertRequest( com.gome.maven.openapi.diff.DiffRequest oldRequest) {
        DiffRequest request = convertRequestFair(oldRequest);
        if (request != null) return request;

        ErrorDiffRequest erorRequest = new ErrorDiffRequest(new MyDiffRequestProducer(oldRequest), "Can't convert from old-style request");
        erorRequest.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new MyShowDiffAction(oldRequest)));
        return erorRequest;
    }

    
    private static DiffRequest convertRequestFair( com.gome.maven.openapi.diff.DiffRequest oldRequest) {
        if (oldRequest.getOnOkRunnable() != null) return null;
        //if (oldRequest.getBottomComponent() != null) return null; // TODO: we need EDT to make this check. Let's ignore bottom component.
        // TODO: migrate layers

        com.gome.maven.openapi.diff.DiffContent[] contents = oldRequest.getContents();
        String[] titles = oldRequest.getContentTitles();
        List<DiffContent> newContents = new ArrayList<DiffContent>(contents.length);

        for (int i = 0; i < contents.length; i++) {
            DiffContent convertedContent = convertContent(oldRequest.getProject(), contents[i]);
            if (convertedContent == null) return null;
            newContents.add(convertedContent);
        }

        SimpleDiffRequest newRequest = new SimpleDiffRequest(oldRequest.getWindowTitle(), newContents, Arrays.asList(titles));

        newRequest.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new MyShowDiffAction(oldRequest)));

        DiffNavigationContext navigationContext = (DiffNavigationContext)oldRequest.getGenericData().get(DiffTool.SCROLL_TO_LINE.getName());
        if (navigationContext != null) {
            newRequest.putUserData(DiffUserDataKeysEx.NAVIGATION_CONTEXT, navigationContext);
        }

        return newRequest;
    }

    
    private static DiffContent convertContent( Project project,  final com.gome.maven.openapi.diff.DiffContent oldContent) {
        if (oldContent.isEmpty()) {
            return new EmptyContent();
        }
        if (oldContent.isBinary()) {
            VirtualFile file = oldContent.getFile();
            if (file == null) return null;
            return new BinaryFileContentImpl(project, file);
        }
        else {
            Document document = oldContent.getDocument();
            if (document == null) return null;
            return new DocumentContentImpl(document, oldContent.getContentType(), oldContent.getFile(), oldContent.getLineSeparator(), null) {
                
                @Override
                public OpenFileDescriptor getOpenFileDescriptor(int offset) {
                    return oldContent.getOpenFileDescriptor(offset);
                }

                @Override
                public void onAssigned(boolean isAssigned) {
                    oldContent.onAssigned(isAssigned);
                }
            };
        }
    }

    private static class ChangeRequestChainWrapper extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
         private final ChangeRequestChain myChain;
         private final List<MyProducerWrapper> myRequests;

        private int myIndex;

        public ChangeRequestChainWrapper( ChangeRequestChain chain) {
            myChain = chain;
            myRequests = ContainerUtil.map(myChain.getAllRequests(),
                    new Function<DiffRequestPresentable, MyProducerWrapper>() {
                        @Override
                        public MyProducerWrapper fun(DiffRequestPresentable presentable) {
                            return new MyProducerWrapper(myChain, presentable);
                        }
                    });

            myIndex = chain.getAllRequests().indexOf(chain.getCurrentRequest());
        }

        
        @Override
        public List<? extends MyProducerWrapper> getRequests() {
            return myRequests;
        }

        @Override
        public int getIndex() {
            return myIndex;
        }

        @Override
        public void setIndex(int index) {
            assert index >= 0 && index < myRequests.size();
            myIndex = index;
        }

        
        private static Change getChange( DiffRequestPresentable presentable) {
            if (presentable instanceof DiffRequestPresentableProxy) {
                try {
                    presentable = ((DiffRequestPresentableProxy)presentable).init();
                }
                catch (VcsException e) {
                    LOG.info(e);
                    return null;
                }
            }
            if (presentable instanceof ChangeDiffRequestPresentable) {
                return ((ChangeDiffRequestPresentable)presentable).getChange();
            }
            return null;
        }

        
        @Override
        public AnAction createGoToChangeAction( Consumer<Integer> onSelected) {
            return new ChangeGoToChangePopupAction<ChangeRequestChainWrapper>(this, onSelected) {
                
                @Override
                protected List<Change> getChanges() {
                    return ContainerUtil.mapNotNull(myChain.getRequests(), new Function<MyProducerWrapper, Change>() {
                        @Override
                        
                        public Change fun(MyProducerWrapper wrapper) {
                            return getChange(wrapper.getPresentable());
                        }
                    });
                }

                
                @Override
                protected Change getCurrentSelection() {
                    return getChange(myChain.getRequests().get(myIndex).getPresentable());
                }

                @Override
                protected int findSelectedStep( Change change) {
                    if (change == null) return -1;
                    for (int i = 0; i < myRequests.size(); i++) {
                        Change c = getChange(myRequests.get(i).getPresentable());
                        if (c != null && change.equals(c)) {
                            return i;
                        }
                    }
                    return -1;
                }
            };
        }
    }

    private static class MyProducerWrapper implements DiffRequestProducer {
         private final DiffRequestPresentable myPresentable;
         private final ChangeRequestChain myChain;

        public MyProducerWrapper( ChangeRequestChain chain,
                                  DiffRequestPresentable presentable) {
            myPresentable = presentable;
            myChain = chain;
        }

        
        @Override
        public String getName() {
            return myPresentable.getPathPresentation();
        }

        
        public DiffRequestPresentable getPresentable() {
            return myPresentable;
        }

        
        @Override
        public DiffRequest process( UserDataHolder context,  ProgressIndicator indicator)
                throws DiffRequestProducerException, ProcessCanceledException {
            com.gome.maven.openapi.diff.DiffRequest oldRequest =
                    UIUtil.invokeAndWaitIfNeeded(new Computable<com.gome.maven.openapi.diff.DiffRequest>() {
                        @Override
                        public com.gome.maven.openapi.diff.DiffRequest compute() {
                            return myChain.moveTo(myPresentable);
                        }
                    });
            if (oldRequest == null) return new ErrorDiffRequest(this, "Can't build old-style request");
            return convertRequest(oldRequest);
        }
    }

    private static class MyShowDiffAction extends DumbAwareAction {
         private final com.gome.maven.openapi.diff.DiffRequest myRequest;

        public MyShowDiffAction( com.gome.maven.openapi.diff.DiffRequest request) {
            super("Show in old diff tool", null, AllIcons.Diff.Diff);
            setEnabledInModalContext(true);
            myRequest = request;
            request.addHint(DO_NOT_TRY_MIGRATE);
        }

        @Override
        public void update(AnActionEvent e) {
            if (!ApplicationManager.getApplication().isInternal()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            e.getPresentation().setVisible(true);
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            DiffManager.getInstance().getDiffTool().show(myRequest);
        }
    }

    private static class MyDiffRequestProducer implements DiffRequestProducer {
         private final com.gome.maven.openapi.diff.DiffRequest myRequest;

        public MyDiffRequestProducer( com.gome.maven.openapi.diff.DiffRequest request) {
            myRequest = request;
        }

        
        @Override
        public String getName() {
            return notNullize(myRequest.getWindowTitle());
        }

        
        @Override
        public DiffRequest process( UserDataHolder context,  ProgressIndicator indicator)
                throws DiffRequestProducerException, ProcessCanceledException {
            ErrorDiffRequest errorRequest = new ErrorDiffRequest(this, "Can't convert from old-style request");
            errorRequest.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new MyShowDiffAction(myRequest)));
            return errorRequest;
        }
    }
}
