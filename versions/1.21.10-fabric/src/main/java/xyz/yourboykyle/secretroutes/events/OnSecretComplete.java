/*
 * Secret Routes Mod - Secret Route Waypoints for Hypixel Skyblock Dungeons
 * Copyright 2025 yourboykyle & R-aMcC
 *
 * <DO NOT REMOVE THIS COPYRIGHT NOTICE>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package xyz.yourboykyle.secretroutes.events;

import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.utils.ChatUtils;
import xyz.yourboykyle.secretroutes.utils.PBUtils;
import xyz.yourboykyle.secretroutes.utils.SecretSounds;

public class OnSecretComplete {
    public static void onSecretCompleteNoKeybind() {
        SecretSounds.secretChime();
        SecretRoutesManager manager = SecretRoutesManager.get();

        if (!manager.hasRoute()) return;

        int index = manager.getCurrentStepIndex();
        int total = manager.getTotalSteps();
        String roomName = manager.getRoomName();

        // PB Logic
        if (index == 0) {
            ChatUtils.sendVerboseMessage("Starting timer for " + roomName, "Personal Bests");
            PBUtils.pbIsValid = true;
            PBUtils.startRoute();
        } else if (index == total - 1) {
            ChatUtils.sendVerboseMessage("Stopping timer for " + roomName, "Personal Bests");
            PBUtils.stopRoute();
        }

        if (index <= total - 1) {
            ChatUtils.sendVerboseMessage("Secret " + (index + 1) + "/" + total + " in " + roomName +
                    " completed in §a" + ((index > 0) ? PBUtils.formatTime(System.currentTimeMillis() - PBUtils.startTime) : "0.000s") +
                    " §r(PB is valid: " + (PBUtils.pbIsValid ? "true" : "false") + ")", "Personal Bests");
        }
    }
}