package com.santidev.biotrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.santidev.biotrack.data.firestore.FirestoreStudiesRepository
import com.santidev.biotrack.ui.composables.auth.AuthViewModel
import com.santidev.biotrack.ui.composables.auth.LoginScreen
import com.santidev.biotrack.ui.composables.studies.adds.AddStudyScreen
import com.santidev.biotrack.ui.composables.studies.adds.AddStudyViewModel
import com.santidev.biotrack.ui.composables.studies.details.StudyDetailScreen
import com.santidev.biotrack.ui.composables.studies.details.StudyDetailViewModel
import com.santidev.biotrack.ui.composables.studies.lists.StudyListScreen
import com.santidev.biotrack.ui.composables.studies.lists.StudyListViewModel
import com.santidev.biotrack.ui.composables.summary.DoctorSummaryScreen
import com.santidev.biotrack.ui.composables.summary.DoctorSummaryViewModel
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

@Composable
fun NavWrapper(authViewModel: AuthViewModel) {
  val user by authViewModel.user.collectAsStateWithLifecycle()
  val currentUser = user
  
  if (currentUser == null) {
    LoginScreen(authViewModel)
  } else {
    val backStack = remember { mutableStateListOf<Any>(Routes.StudyList) }
    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
      ),
      entryProvider = entryProvider {
        entry<Routes.StudyList> {
          val viewModel: StudyListViewModel = viewModel(
            key = currentUser.uid, //un ViewModel distinto por cada cuenta
            factory = viewModelFactory {
              initializer { StudyListViewModel(FirestoreStudiesRepository()) }
            }
          )
          StudyListScreen(
            viewModel = viewModel,
            onAddStudy = { backStack.add(Routes.AddStudy) },
            onOpenStudy = { studyId -> backStack.add(Routes.StudyDetail(studyId)) },
            onSignOut = authViewModel::signOut,
            onOpenSummary = { backStack.add(Routes.DoctorSummary) },
          )
        }
        entry<Routes.AddStudy> {
          val viewModel: AddStudyViewModel = viewModel(
            key = "add-study-${currentUser.uid}",
            factory = viewModelFactory {
              initializer { AddStudyViewModel(FirestoreStudiesRepository()) }
            }
          )
          AddStudyScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onSaved = { backStack.removeLastOrNull() },
          )
        }
        entry<Routes.StudyDetail> { route ->
          val viewModel: StudyDetailViewModel = viewModel(
            key = "detail-${route.studyId}-${currentUser.uid}",
            factory = viewModelFactory {
              initializer { StudyDetailViewModel(FirestoreStudiesRepository(), route.studyId) }
            }
          )
          StudyDetailScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Routes.DoctorSummary> {
          val viewModel: DoctorSummaryViewModel = viewModel(
            key = "summary-${currentUser.uid}",
            factory = viewModelFactory {
              initializer { DoctorSummaryViewModel(FirestoreStudiesRepository()) }
            }
          )
          DoctorSummaryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() }
          )
        }
      }
    )
  }
}