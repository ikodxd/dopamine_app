package com.example.diplom.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.diplom.data.AiRepository
import com.example.diplom.data.Fact
import com.example.diplom.data.SettingsRepository
import com.example.diplom.ui.theme.DiplomTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class InterventionStep {
    BREATHING,
    MATH_CHALLENGE,
    STROOP_TEST,
    FACT_PRESENTATION,
    REFLECTION
}

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var aiRepository: AiRepository
    private lateinit var settingsRepository: SettingsRepository

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        settingsRepository = SettingsRepository(this)
        aiRepository = AiRepository(settingsRepository)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPackage = intent?.getStringExtra("target_package") ?: "Unknown"
        val appName = getAppNameFromPackage(targetPackage)
        showOverlay(targetPackage, appName)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_NOT_STICKY
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun showOverlay(targetPackage: String, appName: String) {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val composeView = ComposeView(this).apply {
            setContent {
                DiplomTheme {
                    FullScreenInterventionContent(
                        targetPackage = targetPackage,
                        targetAppName = appName,
                        aiRepository = aiRepository,
                        onFinished = { 
                            launchApp(targetPackage)
                            stopSelf() 
                        },
                        onCancel = { stopSelf() }
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        windowManager.addView(composeView, params)
        overlayView = composeView
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FullScreenInterventionContent(
    targetPackage: String,
    targetAppName: String,
    aiRepository: AiRepository,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(InterventionStep.BREATHING) }
    var generatedFact by remember { mutableStateOf<Fact?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500)) + scaleIn()).togetherWith(fadeOut(animationSpec = tween(500)))
            },
            label = "StepTransition"
        ) { step ->
            when (step) {
                InterventionStep.BREATHING -> BreathingScreen { currentStep = InterventionStep.MATH_CHALLENGE }
                InterventionStep.MATH_CHALLENGE -> MathChallengeScreen { currentStep = InterventionStep.STROOP_TEST }
                InterventionStep.STROOP_TEST -> StroopTestScreen { currentStep = InterventionStep.FACT_PRESENTATION }
                InterventionStep.FACT_PRESENTATION -> FactStepScreen(aiRepository) { fact -> 
                    generatedFact = fact
                    currentStep = InterventionStep.REFLECTION 
                }
                InterventionStep.REFLECTION -> ReflectionScreen(targetAppName, generatedFact, onFinished, onCancel)
            }
        }
    }
}

@Composable
fun StroopTestScreen(onNext: () -> Unit) {
    val colors = remember {
        listOf(
            "Красный" to Color.Red,
            "Синий" to Color.Blue,
            "Зеленый" to Color.Green,
            "Желтый" to Color.Yellow,
            "Розовый" to Color.Magenta
        )
    }
    
    var correctAnswers by remember { mutableIntStateOf(0) }
    var currentPair by remember { mutableStateOf(generateStroopPair(colors)) }
    
    // Генерируем варианты ответов так, чтобы правильный (цвет шрифта) был всегда один, а остальные были случайными и не повторялись
    val options = remember(currentPair) {
        val correctAnswer = colors.first { it.second == currentPair.second }
        val otherOptions = colors.filter { it.second != currentPair.second }.shuffled().take(2)
        (otherOptions + correctAnswer).shuffled()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Тест Струпа: Нажми на текст цвета шрифта!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(progress = { correctAnswers / 5f }, modifier = Modifier.fillMaxWidth().height(8.dp))
        Text("Верно: $correctAnswers из 5", modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = currentPair.first,
            color = currentPair.second,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        options.forEach { option ->
            Button(
                onClick = {
                    if (option.second == currentPair.second) {
                        correctAnswers++
                        if (correctAnswers >= 5) onNext() else currentPair = generateStroopPair(colors)
                    } else {
                        correctAnswers = 0
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(option.first, fontSize = 18.sp)
            }
        }
    }
}

fun generateStroopPair(colors: List<Pair<String, Color>>): Pair<String, Color> {
    val word = colors.random().first
    val color = colors.random().second
    return Pair(word, color)
}

@Composable
fun FactStepScreen(repository: AiRepository, onNext: (Fact) -> Unit) {
    var fact by remember { mutableStateOf<Fact?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        fact = repository.getRandomFact()
        isLoading = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Генерация факта от ИИ...", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            fact?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(it.category.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it.text, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { onNext(it) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Прочитал")
                }
            }
        }
    }
}

@Composable
fun BreathingScreen(onNext: () -> Unit) {
    var timeLeft by remember { mutableIntStateOf(10) }
    val progress = (10 - timeLeft) / 10f

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Подыши 10 секунд", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
            Text(text = timeLeft.toString(), style = MaterialTheme.typography.displayLarge)
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext, enabled = timeLeft == 0, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (timeLeft > 0) "Вдох-выдох..." else "Дальше")
        }
    }
}

@Composable
fun MathChallengeScreen(onNext: () -> Unit) {
    var solvedCount by remember { mutableIntStateOf(0) }
    var currentProblem by remember { mutableStateOf(generateProblem()) }
    var userAnswer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Реши 5 примеров", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(progress = { solvedCount / 5f }, modifier = Modifier.fillMaxWidth().height(8.dp))
        Spacer(modifier = Modifier.height(48.dp))
        Text("${currentProblem.first} + ${currentProblem.second} = ?", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = userAnswer, onValueChange = { userAnswer = it; isError = false }, isError = isError, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (userAnswer.toIntOrNull() == currentProblem.first + currentProblem.second) {
                solvedCount++
                if (solvedCount >= 5) onNext() else { currentProblem = generateProblem(); userAnswer = "" }
            } else isError = true
        }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Проверить") }
    }
}

fun generateProblem() = Pair(Random.nextInt(1, 40), Random.nextInt(1, 40))

@Composable
fun ReflectionScreen(targetAppName: String, fact: Fact?, onFinished: () -> Unit, onCancel: () -> Unit) {
    val phrases = listOf("Ты уверен?", "Может книгу?", "Присядь 10 раз.", "Помой посуду.", "Выпей воды.", "Мир прекрасен без уведомлений.")
    val randomPhrase = remember { phrases.random() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Info, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        Text(randomPhrase, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Открыть $targetAppName?", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Всё равно войти") }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Я передумал") }
    }
}