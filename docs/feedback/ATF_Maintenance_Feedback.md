<a id="maintenance-of-workspaces---contracts-maintained"></a>
<a id="mentenanța-workspace-urilor--contracte-păstrate"></a>
<a id="atf-j0--feedback-pentru-mentenanța-workspace-urilor"></a>
# Mentenanța workspace-urilor — contracte păstrate

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## Pe scurt

[Mentenanța workspace-urilor](../ATF_Workspace_Synchronization.md#maintenance-increment-protocol) deține procesul întreținut; lacunele funcționale rămân în backlog-urile domeniilor.

<a id="atf-workspace-maintenance-01---gates-and-ownership"></a>
<a id="atf-workspace-maintenance-01--porți-și-ownership"></a>
<a id="atf-workspace-maintenance-01--porți-explicite-și-feedback-trasabil"></a>
## ATF-WORKSPACE-MAINTENANCE-01 — porți și ownership

[Protocolul workspace-urilor](../ATF_Workspace_Synchronization.md#protocolul-fiecărui-increment) și [catalogul testelor](../../atf-tests/README.md#mentenanța-perechii-j0j1) dețin ordinea porților, inspecția read-only și acceptanța separată a consumerului. Controalele Git verificate rămân în [baseline](../ATF_Test_Changes_Baseline.md).

<a id="când-merită-evaluat-langgraph"></a>
<a id="when-should-langgraph-be-considered"></a>
### Când merită evaluat LangGraph?

[Decizia de orchestrare](../ATF_Workspace_Synchronization.md#orchestration-runtime-decision) păstrează diagrama și criteriul operațional. Documentarea unui runtime opțional nu îl transformă într-o dependență a proiectului.
