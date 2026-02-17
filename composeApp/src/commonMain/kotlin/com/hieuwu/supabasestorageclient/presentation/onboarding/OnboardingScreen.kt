package com.hieuwu.supabasestorageclient.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.presentation.onboarding.components.OnboardingPageIndicator
import com.hieuwu.supabasestorageclient.presentation.onboarding.slides.FeaturesSlide
import com.hieuwu.supabasestorageclient.presentation.onboarding.slides.SecurityWarningSlide
import com.hieuwu.supabasestorageclient.presentation.onboarding.slides.WelcomeSlide
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main onboarding screen with horizontal pager for slides
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.totalPages })
    val scope = rememberCoroutineScope()
    
    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> WelcomeSlide()
                    1 -> FeaturesSlide()
                    2 -> SecurityWarningSlide(
                        acknowledged = uiState.securityWarningAcknowledged,
                        onAcknowledgedChange = { viewModel.onSecurityWarningAcknowledged(it) }
                    )
                }
            }
            
            // Bottom Navigation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingPageIndicator(
                    pageCount = uiState.totalPages,
                    currentPage = uiState.currentPage
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button (hidden on security warning page)
                    if (uiState.currentPage != 2) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(uiState.totalPages - 1)
                                }
                            }
                        ) {
                            Text("Skip")
                        }
                    } else {
                        // Empty space to maintain layout
                        Box(modifier = Modifier)
                    }
                    
                    // Next/Get Started button
                    Button(
                        onClick = {
                            if (uiState.currentPage == uiState.totalPages - 1) {
                                // Last page - complete onboarding
                                viewModel.completeOnboarding()
                                onComplete()
                            } else {
                                // Navigate to next page
                                scope.launch {
                                    pagerState.animateScrollToPage(uiState.currentPage + 1)
                                }
                            }
                        },
                        enabled = viewModel.canProceedFromCurrentPage()
                    ) {
                        Text(
                            if (uiState.currentPage == uiState.totalPages - 1) {
                                "Get Started"
                            } else {
                                "Next"
                            }
                        )
                    }
                }
            }
        }
    }
}
