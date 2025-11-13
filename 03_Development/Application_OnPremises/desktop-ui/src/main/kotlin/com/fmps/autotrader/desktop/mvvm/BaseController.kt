package com.fmps.autotrader.desktop.mvvm

import org.koin.core.component.KoinComponent
import tornadofx.Controller

/**
 * Base controller that bridges TornadoFX controllers with Koin dependency injection.
 */
abstract class BaseController : Controller(), KoinComponent


