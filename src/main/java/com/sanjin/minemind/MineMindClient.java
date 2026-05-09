package com.sanjin.minemind;

import com.sanjin.minemind.ai.AiCommands;
import com.sanjin.minemind.ai.AiController;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MineMind.MODID, dist = Dist.CLIENT)
public class MineMindClient {
    public MineMindClient() {
        NeoForge.EVENT_BUS.addListener(AiCommands::register);
        NeoForge.EVENT_BUS.addListener(AiController::onClientChat);
        NeoForge.EVENT_BUS.addListener(AiController::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(AiController::onLoggingOut);
    }
}
