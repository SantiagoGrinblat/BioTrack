package com.santidev.biotrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.santidev.biotrack.data.LabRepository
import com.santidev.biotrack.ui.composables.auth.AuthViewModel
import com.santidev.biotrack.ui.composables.auth.LoginScreen
import com.santidev.biotrack.ui.screens.infoScreen.NewLabScreen
import com.santidev.biotrack.ui.screens.infoScreen.NewLabViewModel
import com.santidev.biotrack.ui.screens.initialScreen.LabListScreen
import com.santidev.biotrack.ui.screens.initialScreen.LabListViewModel
import com.santidev.biotrack.ui.screens.summaryScreen.DoctorSummaryScreen
import com.santidev.biotrack.ui.screens.summaryScreen.DoctorSummaryViewModel

@Composable
fun NavWrapper(authViewModel: AuthViewModel) {
  val user by authViewModel.user.collectAsStateWithLifecycle()
  val currentUser = user
  
  if (currentUser == null) {
    LoginScreen(authViewModel)
  } else {
    val backStack = remember { mutableStateListOf<Any>(Routes.LabList) }
    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
      ),
      entryProvider = entryProvider {
        entry<Routes.LabList> {
          val viewModel: LabListViewModel = viewModel(
            key = currentUser.uid, //un ViewModel distinto por cada cuenta
            factory = viewModelFactory {
              initializer { LabListViewModel(LabRepository()) }
            }
          )
          LabListScreen(
            viewModel = viewModel,
            onNewLab = { backStack.add(Routes.NewLab) },
            onOpenSummary = { backStack.add(Routes.DoctorSummary) },
            onSignOut = authViewModel::signOut,
          )
        }
        entry<Routes.NewLab> {
          val viewModel: NewLabViewModel = viewModel(
            key = "new-lab-${currentUser.uid}",
            factory = viewModelFactory {
              initializer { NewLabViewModel(LabRepository()) }
            }
          )
          NewLabScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onSaved = { backStack.removeLastOrNull() },
          )
        }
        entry<Routes.DoctorSummary> {
          val viewModel: DoctorSummaryViewModel = viewModel(
            key = "summary-${currentUser.uid}",
            factory = viewModelFactory {
              initializer { DoctorSummaryViewModel(LabRepository()) }
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