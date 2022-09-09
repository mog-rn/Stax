package com.hover.stax.presentation.sims

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.hover.sdk.sims.SimInfo
import com.hover.stax.R
import com.hover.stax.domain.model.Account
import com.hover.stax.domain.use_case.sims.SimWithAccount
import com.hover.stax.permissions.PermissionUtils
import com.hover.stax.presentation.home.BalanceTapListener
import com.hover.stax.presentation.home.components.TopBar
import com.hover.stax.presentation.sims.components.LinkSimCard
import com.hover.stax.presentation.sims.components.SampleSimInfoProvider
import com.hover.stax.presentation.sims.components.SimItem
import com.hover.stax.ui.theme.*
import com.hover.stax.utils.Utils
import com.hover.stax.utils.network.NetworkMonitor
import org.koin.androidx.compose.getViewModel
import timber.log.Timber

data class SimScreenClickFunctions(
    val onClickedAddNewAccount: () -> Unit,
    val onClickedSettingsIcon: () -> Unit,
    val onClickedBuyAirtime: () -> Unit
)

private fun hasGratedSimPermission(context: Context) = PermissionUtils.hasContactPermission(context) && PermissionUtils.hasSmsPermission(context)

@Composable
fun SimScreen(
    simScreenClickFunctions: SimScreenClickFunctions,
    balanceTapListener: BalanceTapListener,
    simViewModel: SimViewModel = getViewModel()
) {
    val sims by simViewModel.sims.collectAsState()
    val hasNetwork by NetworkMonitor.StateLiveData.get().observeAsState(initial = false)

    val context = LocalContext.current

    StaxTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Scaffold(
                topBar = {
                    TopBar(
                        title = R.string.your_sim_cards,
                        isInternetConnected = hasNetwork,
                        simScreenClickFunctions.onClickedSettingsIcon,
                        {}
                    )
                },
                content = { innerPadding ->
                    val paddingModifier = Modifier.padding(innerPadding)

                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(id = R.dimen.margin_13))
                            .then(paddingModifier)
                    ) {
                        if (simViewModel.loading) {
                            Timber.i("Status: Loading")
                            item {
                                NoticeText(stringRes = R.string.loading)
                            }
                        }

                        else if (sims.isEmpty()) {
                            Timber.i("Status: Detected empty sims")
                            item {
                                if (hasGratedSimPermission(context)) NoticeText(stringRes = R.string.simpage_empty_sims)
                                else ShowGrantPermissionContent(simScreenClickFunctions.onClickedAddNewAccount)
                            }
                        }

                        else {
                            items(sims.sortedBy { it.sim.slotIdx }) { sim ->
                                Timber.i("Status: Detected a supported sim")
//                                val bonus = simUiState.bonuses.firstOrNull()
                                SimItem(
                                    simWithAccount = sim,
//                                    secondaryClickItem = simScreenClickFunctions.onClickedBuyAirtime,
                                    balanceTapListener = balanceTapListener
                                )
                            }
                        }
                    }
                })
        }
    }
}

@Preview
@Composable
private fun SimScreenPreview(@PreviewParameter(SampleSimInfoProvider::class) sims: List<SimWithAccount>) {
    StaxTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Scaffold(topBar = {
                TopBar(title = R.string.your_sim_cards, isInternetConnected = false, {}, {})
            }, content = { innerPadding ->
                val paddingModifier = Modifier.padding(innerPadding)

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = R.dimen.margin_13))
                        .then(paddingModifier)
                ) {

                    if (sims.isEmpty())
                        item {
                            NoticeText(stringRes = R.string.simpage_permission_alert)
                        }

                    sims.let {
                        if (it.isEmpty()) {
                            item {
                                LinkSimCard(id = R.string.link_sim_to_stax, onClickedLinkSimCard = { })
                            }
                        }
                    }

                    itemsIndexed(sims) { index, sim ->
                        SimItem(
                            simWithAccount = sim,
                            balanceTapListener = null
                        )
                    }
                }
            })
        }
    }
}

@Composable
private fun NoticeText(@StringRes stringRes: Int) {
    val size13 = dimensionResource(id = R.dimen.margin_13)
    val size34 = dimensionResource(id = R.dimen.margin_34)

    Text(
        text = stringResource(id = stringRes),
        modifier = Modifier
            .padding(vertical = size13, horizontal = size34)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = TextGrey,
    )
}

@Composable
private fun ShowGrantPermissionContent( onClickedAddNewAccount: () -> Unit ) {
    Column {
        NoticeText(stringRes = R.string.simpage_permission_alert)
        LinkSimCard(
            id = R.string.link_sim_to_stax,
            onClickedLinkSimCard = onClickedAddNewAccount
        )
    }
}




