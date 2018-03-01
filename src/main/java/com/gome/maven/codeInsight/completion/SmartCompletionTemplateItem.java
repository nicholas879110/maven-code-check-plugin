package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupItem;
import com.gome.maven.codeInsight.lookup.TypedLookupItem;
import com.gome.maven.codeInsight.template.Template;
import com.gome.maven.codeInsight.template.impl.TemplateImpl;
import com.gome.maven.psi.JavaPsiFacade;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiType;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author peter
 */
public class SmartCompletionTemplateItem extends LookupItem<Template> implements TypedLookupItem {
     private static final String PLACEHOLDER = "xxx";
    private final PsiElement myContext;

    public SmartCompletionTemplateItem(Template o, PsiElement context) {
        super(o, o.getKey());
        myContext = context;
    }


    @Override
    public PsiType getType() {
        final Template template = getObject();
        String text = template.getTemplateText();
        StringBuilder resultingText = new StringBuilder(text);

        int segmentsCount = template.getSegmentsCount();

        for (int j = segmentsCount - 1; j >= 0; j--) {
            if (template.getSegmentName(j).equals(TemplateImpl.END)) {
                continue;
            }

            int segmentOffset = template.getSegmentOffset(j);

            resultingText.insert(segmentOffset, PLACEHOLDER);
        }

        try {
            final PsiExpression templateExpression = JavaPsiFacade.getElementFactory(myContext.getProject()).createExpressionFromText(resultingText.toString(), myContext);
            return templateExpression.getType();
        }
        catch (IncorrectOperationException e) { // can happen when text of the template does not form an expression
            return null;
        }
    }
}
