package org.EcoShitter.chestcounter2.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

public class Chestcounter2Client implements ClientModInitializer {

    private boolean wasPressed = false;

    // Move these to fields so you can stop them from anywhere
    private ScheduledExecutorService scheduler;
    private AtomicInteger index;
    private Path file;
    private static final AtomicBoolean stopFlag = new AtomicBoolean(false);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && isKeyPressed(GLFW.GLFW_KEY_J)) {
                Path file = Paths.get("C:/Users/trevorla/Desktop", "pw.txt");
                if (!Files.exists(file)) {
                    client.player.sendMessage(Text.literal("§epw.txt not found"), false);
                    return;
                }

                // reset your stop flag and index
                stopFlag.set(false);
                AtomicInteger index = new AtomicInteger(0);

                // single-threaded scheduler
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                Runnable sendNext = new Runnable() {
                    @Override
                    public void run() {
                        // check your stop condition first
                        if (stopFlag.get()) {
                            scheduler.shutdownNow();
                            return;
                        }

                        try {
                            List<String> passwords = Files.readAllLines(file, StandardCharsets.UTF_8);
                            int i = index.getAndIncrement();
                            if (i < passwords.size()) {
                                String pw = passwords.get(i);
                                MinecraftClient.getInstance().execute(() ->
                                        MinecraftClient.getInstance()
                                                .getNetworkHandler()
                                                .sendChatCommand("cunlock " + pw)
                                );

                                // schedule **this** Runnable again after a fresh random pause
                                long delay = ThreadLocalRandom.current().nextLong(500, 750);
                                scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);

                            } else {
                                // we’re done
                                scheduler.shutdown();
                            }

                        } catch (IOException e) {
                            MinecraftClient.getInstance().execute(() ->
                                    MinecraftClient.getInstance().player.sendMessage(
                                            Text.literal("§cI/O error: " + e.getMessage()), false
                                    )
                            );
                            scheduler.shutdown();
                        }
                    }
                };

                // **only** schedule the first run (no execute(), no fixed-rate)
                long initialDelay = ThreadLocalRandom.current().nextLong(500, 750);
                scheduler.schedule(sendNext, initialDelay, TimeUnit.MILLISECONDS);
            }

            if (isKeyPressed(GLFW.GLFW_KEY_K) &&
                    scheduler != null &&
                    !scheduler.isShutdown()
            ) {
                stopFlag.set(true);
                //scheduler.shutdownNow();
                client.player.sendMessage(
                        Text.literal("§ePassword spray stopped."), false
                );
            }
            wasPressed = false;
        });
    }

    private boolean isKeyPressed(int key) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isCurrentlyPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;

        if (isCurrentlyPressed && !wasPressed) {
            wasPressed = true;
            return true;
        } else if (!isCurrentlyPressed) {
            wasPressed = false;
        }
        return false;
    }
}

/*

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public class Chestcounter2Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) {
                var block = world.getBlockState(hitResult.getBlockPos()).getBlock();

                if (block == Blocks.CHEST) {
                    MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("hi");
                }
            }
            return ActionResult.PASS;
        });
    }
}

 */