package com.example.plugins

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.AutocompleteSuggestion
import com.example.core.model.SuggestionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PluginCategory(val displayName: String) {
    COMPOSE_UI("Compose & UI"),
    COROUTINES("Coroutines & Async"),
    PERSISTENCE("Database & Room"),
    NETWORKING("Networking & Ktor"),
    ARCHITECTURE("Architecture")
}

data class IdePlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: PluginCategory,
    val isEnabled: Boolean = true,
    val snippets: List<AutocompleteSuggestion> = emptyList()
)

class PluginManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("droidide_plugins", Context.MODE_PRIVATE)

    private val defaultPlugins = listOf(
        IdePlugin(
            id = "coroutines_flow_pack",
            name = "Kotlin Coroutines & Flow Pro",
            version = "1.4.0",
            author = "JetBrains Community",
            description = "Productivity snippets for MutableStateFlow, Channel, viewModelScope, and Flow operators.",
            category = PluginCategory.COROUTINES,
            isEnabled = true,
            snippets = listOf(
                AutocompleteSuggestion("StateFlow", "private val _state = MutableStateFlow(InitialState())\nval state: StateFlow<MyState> = _state.asStateFlow()", "StateFlow declaration", SuggestionCategory.KEYWORD),
                AutocompleteSuggestion("viewModelScope.launch", "viewModelScope.launch {\n    \n}", "Coroutine launch block", SuggestionCategory.KEYWORD),
                AutocompleteSuggestion("collectAsStateWithLifecycle", "val uiState by viewModel.state.collectAsStateWithLifecycle()", "Lifecycle-aware state collection", SuggestionCategory.KEYWORD)
            )
        ),
        IdePlugin(
            id = "compose_animations",
            name = "Compose Animation Wizard",
            version = "2.1.0",
            author = "AndroidX Tools",
            description = "High performance Compose transition and animation snippets.",
            category = PluginCategory.COMPOSE_UI,
            isEnabled = true,
            snippets = listOf(
                AutocompleteSuggestion("AnimatedVisibility", "AnimatedVisibility(visible = isVisible) {\n    \n}", "Animated enter/exit visibility", SuggestionCategory.COMPOSABLE),
                AutocompleteSuggestion("animateDpAsState", "val size by animateDpAsState(targetValue = targetDp, label = \"dpAnim\")", "Smooth DP transition", SuggestionCategory.KEYWORD),
                AutocompleteSuggestion("rememberInfiniteTransition", "val transition = rememberInfiniteTransition(label = \"infAnim\")", "Continuous looping animation", SuggestionCategory.KEYWORD)
            )
        ),
        IdePlugin(
            id = "room_query_helper",
            name = "Room DB SQLite Query Pack",
            version = "1.2.0",
            author = "Google Android",
            description = "Room Entity, DAO, and TypeConverter code generation snippets.",
            category = PluginCategory.PERSISTENCE,
            isEnabled = true,
            snippets = listOf(
                AutocompleteSuggestion("@Entity", "@Entity(tableName = \"items\")\ndata class ItemEntity(\n    @PrimaryKey val id: String,\n    val name: String\n)", "Room Entity definition", SuggestionCategory.KEYWORD),
                AutocompleteSuggestion("@Dao", "@Dao\ninterface ItemDao {\n    @Query(\"SELECT * FROM items\")\n    fun getAll(): Flow<List<ItemEntity>>\n\n    @Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun insert(item: ItemEntity)\n}", "Room Data Access Object", SuggestionCategory.KEYWORD)
            )
        ),
        IdePlugin(
            id = "ktor_client_kit",
            name = "Ktor & HTTP Client Kit",
            version = "1.0.5",
            author = "DroidIDE Labs",
            description = "Multiplatform HTTP requests and JSON serialization handlers.",
            category = PluginCategory.NETWORKING,
            isEnabled = true,
            snippets = listOf(
                AutocompleteSuggestion("HttpClient", "val client = HttpClient(OkHttp) {\n    install(ContentNegotiation) {\n        json()\n    }\n}", "Ktor HTTP client setup", SuggestionCategory.KEYWORD),
                AutocompleteSuggestion("client.get", "val response = client.get(\"https://api.example.com/data\")", "GET request call", SuggestionCategory.KEYWORD)
            )
        ),
        IdePlugin(
            id = "m3_color_palette",
            name = "Material 3 Design Tokens",
            version = "3.0.0",
            author = "Material Design",
            description = "ColorScheme and shape elevation token generators.",
            category = PluginCategory.COMPOSE_UI,
            isEnabled = true,
            snippets = listOf(
                AutocompleteSuggestion("MaterialTheme.colorScheme", "MaterialTheme.colorScheme.primaryContainer", "M3 semantic color", SuggestionCategory.KEYWORD),
                AutocompleteSuggestion("RoundedCornerShape", "shape = RoundedCornerShape(16.dp)", "Rounded corner modifier", SuggestionCategory.MODIFIER)
            )
        )
    )

    private val _plugins = MutableStateFlow<List<IdePlugin>>(loadPlugins())
    val plugins: StateFlow<List<IdePlugin>> = _plugins.asStateFlow()

    private fun loadPlugins(): List<IdePlugin> {
        return defaultPlugins.map { plugin ->
            val enabled = prefs.getBoolean("plugin_${plugin.id}", plugin.isEnabled)
            plugin.copy(isEnabled = enabled)
        }
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        prefs.edit().putBoolean("plugin_$pluginId", enabled).apply()
        _plugins.value = _plugins.value.map {
            if (it.id == pluginId) it.copy(isEnabled = enabled) else it
        }
    }

    fun getActiveSnippets(): List<AutocompleteSuggestion> {
        return _plugins.value.filter { it.isEnabled }.flatMap { it.snippets }
    }
}
