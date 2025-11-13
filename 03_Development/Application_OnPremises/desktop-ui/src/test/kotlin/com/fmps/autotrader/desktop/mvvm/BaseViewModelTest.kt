package com.fmps.autotrader.desktop.mvvm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @Test
    fun `state updates are propagated`() = runTest {
        val viewModel = TestViewModel(testDispatcherProvider())
        viewModel.increment()
        assertEquals(1, viewModel.state.value.counter)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `events are emitted`() = runTest {
        val viewModel = TestViewModel(testDispatcherProvider())

        val toast = withTimeout(1_000) {
            viewModel.events
                .onSubscription { viewModel.notify("hello") }
                .first() as TestEvent.Toast
        }
        assertEquals("hello", toast.message)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    private fun TestScope.testDispatcherProvider(): DispatcherProvider {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val computation = dispatcher
        }
    }

    private class TestViewModel(
        dispatcherProvider: DispatcherProvider
    ) : BaseViewModel<TestState, TestEvent>(TestState(), dispatcherProvider) {

        fun increment() {
            setState { it.copy(counter = it.counter + 1) }
        }

        fun notify(message: String) {
            publishEvent(TestEvent.Toast(message))
        }
    }

    private data class TestState(val counter: Int = 0)

    private sealed interface TestEvent : ViewEvent {
        data class Toast(val message: String) : TestEvent
    }
}

