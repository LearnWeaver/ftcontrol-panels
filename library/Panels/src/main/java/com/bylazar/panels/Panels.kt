package com.bylazar.panels

import android.content.Context
import com.bylazar.panels.core.TextHandler
import com.bylazar.panels.core.OpModeHandler
import com.bylazar.panels.core.PreferencesHandler
import com.bylazar.panels.reflection.ClassFinder
import com.bylazar.panels.server.Socket
import com.bylazar.panels.server.StaticServer
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier.Notifications
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar
import com.qualcomm.robotcore.util.WebHandlerManager
import org.firstinspires.ftc.ftccommon.external.OnCreate
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import org.firstinspires.ftc.ftccommon.external.OnDestroy
import org.firstinspires.ftc.ftccommon.external.WebHandlerRegistrar
import org.firstinspires.ftc.ftccommon.internal.FtcRobotControllerWatchdogService
import org.firstinspires.ftc.robotcore.internal.system.AppUtil


object Panels : Notifications {
    lateinit var server: StaticServer
    lateinit var socket: Socket
    var config = PanelsConfig()

    @JvmStatic
    @OpModeRegistrar
    fun registerOpMode(manager: OpModeManager) {
        OpModeHandler.init(manager)
        OpModeHandler.registerOpMode()
    }

    @JvmStatic
    @OnCreate
    fun start(context: Context) {
        PreferencesHandler.init(context)
        enable()
        TextHandler.injectText()

        val configs = ClassFinder().findClasses(
            apkPath = context.packageCodePath,
            shouldKeepFilter = { clazz ->
                PanelsConfig::class.java.isAssignableFrom(clazz) && clazz != PanelsConfig::class.java
            }
        )

        Logger.coreLog("Found ${configs.size} configs.")

        if (configs.isNotEmpty()) {
            config = Class.forName(configs[0].className)
                .getDeclaredConstructor()
                .newInstance() as PanelsConfig
        }
        Logger.coreLog("Config is $config")

        if (config.isDisabled) {
            PreferencesHandler.isEnabled = false
            disable()
        }
    }

    @JvmStatic
    @WebHandlerRegistrar
    fun attachWebServer(context: Context, manager: WebHandlerManager) {
        try {
            server = StaticServer(context, 8001, "web")
            socket = Socket(8002)
        } catch (e: Exception) {
            Logger.coreLog("Failed to start webserver: " + e.message)
        }

        if (PreferencesHandler.isEnabled) {
            server.startServer()
            socket.startServer()
        }
    }

    @JvmStatic
    @OnCreateEventLoop
    fun attachEventLoop(context: Context, eventLoop: FtcEventLoop?) {

    }

    @JvmStatic
    @OnDestroy
    fun stop(context: Context) {
        if (!FtcRobotControllerWatchdogService.isLaunchActivity(
                AppUtil.getInstance().rootActivity
            )
        ) {
            return
        }

        TextHandler.removeText()
        server.stopServer()
        socket.stopServer()
    }

    override fun onOpModePreInit(opMode: OpMode) {

    }

    override fun onOpModePreStart(opMode: OpMode) {

    }

    override fun onOpModePostStop(opMode: OpMode) {

    }


    fun toggle() {
        when (PreferencesHandler.isEnabled) {
            true -> disable()
            false -> enable()
        }
    }

    fun enable() {
        if (PreferencesHandler.isEnabled) return
        PreferencesHandler.isEnabled = true
        server.startServer()
        socket.startServer()
    }

    fun disable() {
        if (!PreferencesHandler.isEnabled) return
        PreferencesHandler.isEnabled = false
        server.stopServer()
        socket.stopServer()
    }
}