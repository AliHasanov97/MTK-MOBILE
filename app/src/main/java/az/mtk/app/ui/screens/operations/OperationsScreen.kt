package az.mtk.app.ui.screens.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.ui.mtkViewModel

enum class OperationsTab(val label: String, val subtitle: String) {
    CONTRACTS("Müqavilələr", "Tədarükçü və müştəri müqavilələri"),
    VENDORS("Tədarükçülər", "Tədarükçülər və qarşılıqlı borclar"),
    CUSTOMERS("Müştərilər", "Müştərilər və onların borcları"),
    DEBTS("Borclar", "Sakinlər üzrə açıq haqlar və ödənişlər"),
    FINANCE("Maliyyə", "Gəlir, xərc, tarif və əmək haqqı"),
}

/**
 * Əməliyyatlar modulu — saytdakı 5 bölmənin mobil-native qarşılığı.
 * Cədvəl yoxdur: kartlar, seqmentlər, bottom-sheet formaları və tam ekran
 * detallar. Daxili naviqasiya bu ekranda saxlanılır ki, sistem "geri"
 * düyməsi əvvəlcə detalı, sonra modulu bağlasın.
 */
@Composable
fun OperationsScreen(
    onBack: () -> Unit,
    canManage: Boolean,
    canPay: Boolean,
    isAdmin: Boolean,
    contractsViewModel: ContractsViewModel = mtkViewModel { ContractsViewModel() },
    partiesViewModel: PartiesViewModel = mtkViewModel { PartiesViewModel() },
    debtsViewModel: DebtsViewModel = mtkViewModel { DebtsViewModel() },
    financeViewModel: FinanceViewModel = mtkViewModel { FinanceViewModel() },
) {
    var tab by remember { mutableStateOf(OperationsTab.CONTRACTS) }
    var contractId by remember { mutableStateOf<String?>(null) }
    var partyId by remember { mutableStateOf<String?>(null) }

    val contractsState by contractsViewModel.state.collectAsStateWithLifecycle()
    val partiesState by partiesViewModel.state.collectAsStateWithLifecycle()
    val debtsState by debtsViewModel.state.collectAsStateWithLifecycle()
    val financeState by financeViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(canManage, canPay, isAdmin) {
        contractsViewModel.setCanManage(canManage)
        contractsViewModel.setCanPay(canPay)
        partiesViewModel.setCanManage(canManage)
        partiesViewModel.setCanPay(canPay)
        debtsViewModel.setPermissions(canManage, canPay, isAdmin)
    }

    LaunchedEffect(tab) {
        when (tab) {
            OperationsTab.CONTRACTS ->
                if (contractsState.list.isEmpty() && !contractsState.loading) contractsViewModel.load()
            OperationsTab.VENDORS -> partiesViewModel.setKind(PartyKind.VENDOR)
            OperationsTab.CUSTOMERS -> partiesViewModel.setKind(PartyKind.CUSTOMER)
            OperationsTab.DEBTS ->
                if (debtsState.charges.isEmpty() && !debtsState.loading) debtsViewModel.load()
            OperationsTab.FINANCE -> Unit
        }
    }

    BackHandler(enabled = true) {
        when {
            contractId != null -> {
                contractsViewModel.closeDetail()
                contractId = null
            }
            partyId != null -> {
                partiesViewModel.closeDetail()
                partyId = null
            }
            financeState.section != null -> financeViewModel.closeSection()
            else -> onBack()
        }
    }

    when {
        contractId != null -> ContractDetailScreen(
            contractId = contractId!!,
            viewModel = contractsViewModel,
            onBack = {
                contractsViewModel.closeDetail()
                contractId = null
            },
            onOpenContract = { newId ->
                contractsViewModel.closeDetail()
                contractId = newId
                contractsViewModel.openContract(newId)
            },
        )

        partyId != null -> PartyDetailScreen(
            partyId = partyId!!,
            viewModel = partiesViewModel,
            onBack = {
                partiesViewModel.closeDetail()
                partyId = null
            },
            onOpenContract = { id ->
                contractId = id
                contractsViewModel.openContract(id)
            },
        )

        financeState.section != null -> FinanceSectionScreen(
            section = financeState.section!!,
            viewModel = financeViewModel,
            onBack = { financeViewModel.closeSection() },
            canManage = canManage,
            canPay = canPay,
        )

        else -> {
            val subtitle = tab.subtitle
            Scaffold(
                containerColor = OpsColors.Background,
                topBar = {
                    OpsTopBar(
                        title = "Əməliyyatlar",
                        subtitle = subtitle,
                        onBack = onBack,
                        onRefresh = {
                            when (tab) {
                                OperationsTab.CONTRACTS -> contractsViewModel.load()
                                OperationsTab.VENDORS, OperationsTab.CUSTOMERS -> partiesViewModel.load()
                                OperationsTab.DEBTS -> debtsViewModel.load()
                                OperationsTab.FINANCE -> Unit
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Spacer(Modifier.height(2.dp))
                    OpsPillRow(
                        pills = listOf(
                            OpsPill(
                                label = OperationsTab.CONTRACTS.label,
                                selected = tab == OperationsTab.CONTRACTS,
                                count = contractsState.total.takeIf { it > 0 },
                                onClick = { tab = OperationsTab.CONTRACTS },
                            ),
                            OpsPill(
                                label = OperationsTab.VENDORS.label,
                                selected = tab == OperationsTab.VENDORS,
                                count = partiesState.total.takeIf {
                                    it > 0 && partiesState.kind == PartyKind.VENDOR
                                },
                                onClick = { tab = OperationsTab.VENDORS },
                            ),
                            OpsPill(
                                label = OperationsTab.CUSTOMERS.label,
                                selected = tab == OperationsTab.CUSTOMERS,
                                count = partiesState.total.takeIf {
                                    it > 0 && partiesState.kind == PartyKind.CUSTOMER
                                },
                                onClick = { tab = OperationsTab.CUSTOMERS },
                            ),
                            OpsPill(
                                label = OperationsTab.DEBTS.label,
                                selected = tab == OperationsTab.DEBTS,
                                count = debtsState.total.takeIf { it > 0 },
                                onClick = { tab = OperationsTab.DEBTS },
                            ),
                            OpsPill(
                                label = OperationsTab.FINANCE.label,
                                selected = tab == OperationsTab.FINANCE,
                                onClick = { tab = OperationsTab.FINANCE },
                            ),
                        ),
                    )
                    Spacer(Modifier.height(8.dp))

                    when (tab) {
                        OperationsTab.CONTRACTS -> ContractsTab(
                            state = contractsState,
                            viewModel = contractsViewModel,
                            onOpenContract = { id ->
                                contractId = id
                                contractsViewModel.openContract(id)
                            },
                        )
                        OperationsTab.VENDORS, OperationsTab.CUSTOMERS -> PartiesTab(
                            state = partiesState,
                            viewModel = partiesViewModel,
                            onOpenParty = { id ->
                                partyId = id
                                partiesViewModel.openParty(id)
                            },
                        )
                        OperationsTab.DEBTS -> DebtsTab(
                            state = debtsState,
                            viewModel = debtsViewModel,
                        )
                        OperationsTab.FINANCE -> FinanceHubTab(
                            viewModel = financeViewModel,
                        )
                    }
                }
            }
        }
    }
}
