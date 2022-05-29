package com.github.lppedd.cc.completion.providers

import com.github.lppedd.cc.CC
import com.github.lppedd.cc.api.CommitSubject
import com.github.lppedd.cc.api.CommitSubjectProvider
import com.github.lppedd.cc.api.CommitTokenProviderService
import com.github.lppedd.cc.completion.resultset.ResultSet
import com.github.lppedd.cc.lookupElement.CommitSubjectLookupElement
import com.github.lppedd.cc.parser.CommitContext.SubjectCommitContext
import com.github.lppedd.cc.psiElement.CommitSubjectPsiElement
import com.github.lppedd.cc.safeRunWithCheckCanceled
import com.github.lppedd.cc.vcs.RecentCommitsService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * @author Edoardo Luppi
 */
internal class SubjectCompletionProvider(
    private val project: Project,
    private val context: SubjectCommitContext,
) : CompletionProvider<CommitSubjectProvider> {
  override fun getProviders(): Collection<CommitSubjectProvider> =
    project.service<CommitTokenProviderService>().getSubjectProviders()

  override fun stopHere(): Boolean =
    false

  override fun complete(resultSet: ResultSet) {
    val prefixedResultSet = resultSet.withPrefixMatcher(context.subject.trimStart())
    val recentCommitsService = project.service<RecentCommitsService>()
    val recentSubjects = recentCommitsService.getRecentSubjects()
    val subjects = LinkedHashSet<ProviderCommitToken<CommitSubject>>(64)

    getProviders().forEach { provider ->
      safeRunWithCheckCanceled {
        provider.getCommitSubjects(context.type, context.scope)
          .asSequence()
          .take(CC.Provider.MaxItems)
          .forEach { subjects.add(ProviderCommitToken(provider, it)) }
      }
    }

    subjects.forEachIndexed { index, (provider, commitSubject) ->
      val psiElement = CommitSubjectPsiElement(project, commitSubject.getText())
      val element = CommitSubjectLookupElement(psiElement, commitSubject)
      element.putUserData(ELEMENT_INDEX, index)
      element.putUserData(ELEMENT_PROVIDER, provider)
      element.putUserData(ELEMENT_IS_RECENT, recentSubjects.contains(commitSubject.getValue()))
      prefixedResultSet.addElement(element)
    }
  }
}
