package com.github.lppedd.cc.completion.providers

import com.github.lppedd.cc.CC
import com.github.lppedd.cc.api.CommitFooterValue
import com.github.lppedd.cc.api.CommitFooterValueProvider
import com.github.lppedd.cc.api.CommitTokenProviderService
import com.github.lppedd.cc.completion.resultset.ResultSet
import com.github.lppedd.cc.lookupElement.CommitFooterValueLookupElement
import com.github.lppedd.cc.lookupElement.ShowMoreCoAuthorsLookupElement
import com.github.lppedd.cc.parser.CommitTokens
import com.github.lppedd.cc.parser.FooterContext.FooterValueContext
import com.github.lppedd.cc.parser.ValidToken
import com.github.lppedd.cc.psiElement.CommitFooterValuePsiElement
import com.github.lppedd.cc.safeRunWithCheckCanceled
import com.github.lppedd.cc.vcs.RecentCommitsService
import com.intellij.codeInsight.completion.CompletionProcess
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * @author Edoardo Luppi
 */
internal class FooterValueCompletionProvider(
    private val project: Project,
    private val context: FooterValueContext,
    private val commitTokens: CommitTokens,
    private val process: CompletionProcess,
) : CompletionProvider<CommitFooterValueProvider> {
  override fun getProviders(): Collection<CommitFooterValueProvider> =
    project.service<CommitTokenProviderService>().getFooterValueProviders()

  override fun stopHere(): Boolean =
    true

  override fun complete(resultSet: ResultSet) {
    val prefix = context.value.trimStart()
    val prefixedResultSet = resultSet.withPrefixMatcher(prefix)
    val recentCommitsService = project.service<RecentCommitsService>()
    val recentFooterValues = recentCommitsService.getRecentFooterValues()
    val footerValues = LinkedHashSet<ProviderCommitToken<CommitFooterValue>>(64)

    // See comment in TypeCompletionProvider
    getProviders().forEach { provider ->
      safeRunWithCheckCanceled {
        val commitFooterValues = provider.getCommitFooterValues(
            context.type,
            (commitTokens.type as? ValidToken)?.value,
            (commitTokens.scope as? ValidToken)?.value,
            (commitTokens.subject as? ValidToken)?.value,
        )

        commitFooterValues.asSequence()
          .take(CC.Provider.MaxItems)
          .forEach { footerValues.add(ProviderCommitToken(provider, it)) }
      }
    }

    footerValues.forEachIndexed { index, (provider, commitFooterValue) ->
      val psiElement = CommitFooterValuePsiElement(project, commitFooterValue.getText())
      val element = CommitFooterValueLookupElement(psiElement, commitFooterValue)
      element.putUserData(ELEMENT_INDEX, index)
      element.putUserData(ELEMENT_PROVIDER, provider)
      element.putUserData(ELEMENT_IS_RECENT, recentFooterValues.contains(commitFooterValue.getValue()))
      prefixedResultSet.addElement(element)
    }

    if ("co-authored-by".equals(context.type, true)) {
      val element = ShowMoreCoAuthorsLookupElement(project, prefix)
      element.putUserData(ELEMENT_INDEX, Int.MAX_VALUE)

      @Suppress("UnstableApiUsage")
      if (process is CompletionProgressIndicator) {
        process.lookup.addPrefixChangeListener(element, process.lookup)
      }

      prefixedResultSet.addElement(element)
    }
  }
}
