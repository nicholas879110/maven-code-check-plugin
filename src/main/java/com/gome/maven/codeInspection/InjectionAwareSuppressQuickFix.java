package com.gome.maven.codeInspection;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.ThreeState;

/**
 * This kind of suppression fix allows to clients to specify whether the fix should
 * be invoked on injected elements or on elements of host files.
 * <p/>
 * By default suppression fixes on injected elements are able to make suppression inside injection only.
 * Whereas implementation of this interface will be provided for suppressing inside injection and in injection host.
 * See {@link InspectionProfileEntry#getBatchSuppressActions(PsiElement)} for details.
 */
public interface InjectionAwareSuppressQuickFix extends SuppressQuickFix {
    ThreeState isShouldBeAppliedToInjectionHost();

    void setShouldBeAppliedToInjectionHost(ThreeState shouldBeAppliedToInjectionHost);
}
