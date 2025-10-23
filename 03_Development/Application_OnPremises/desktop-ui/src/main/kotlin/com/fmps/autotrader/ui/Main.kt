package com.fmps.autotrader.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class AutoTraderApp : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.title = "FMPS AutoTrader v1.0.0-SNAPSHOT"
        
        val label = Label("FMPS AutoTrader Desktop UI")
        val root = StackPane(label)
        
        primaryStage.scene = Scene(root, 800.0, 600.0)
        primaryStage.show()
        
        println("Desktop UI started")
    }
}

fun main(args: Array<String>) {
    Application.launch(AutoTraderApp::class.java, *args)
}

