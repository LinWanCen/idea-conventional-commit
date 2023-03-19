package com.github.lppedd.cc.language.psi

import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiPlainText
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

/**
 * @author Edoardo Luppi
 */
class ConventionalCommitFooterValuePsiElement(
    type: IElementType,
    text: CharSequence,
) : LeafPsiElement(type, text), PsiPlainText {
  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is ConventionalCommitPsiElementVisitor) {
      visitor.visitFooterValue(this)
    } else {
      super.accept(visitor)
    }
  }

  override fun toString(): String =
    "ConventionalCommitFooterValuePsiElement"
}
