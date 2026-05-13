package com.sanjin.minemind.ai;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AiScreenshot {
    private static final ExecutorService ENCODER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MineMind Screenshot Encoder");
        thread.setDaemon(true);
        return thread;
    });

    private AiScreenshot() {
    }

    public static CompletableFuture<AiImageAttachment> capture(String quality) {
        CompletableFuture<AiImageAttachment> future = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getMainRenderTarget() == null) {
            future.completeExceptionally(new ScreenshotException());
            return future;
        }

        Runnable task = () -> {
            try {
                Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image ->
                        ENCODER.execute(() -> encodeAndComplete(image, quality, future))
                );
            } catch (RuntimeException exception) {
                future.completeExceptionally(new ScreenshotException(exception));
            }
        };
        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.execute(task);
        }
        return future;
    }

    private static void encodeAndComplete(NativeImage image, String quality, CompletableFuture<AiImageAttachment> future) {
        try (image) {
            future.complete(encode(image, AiImageQuality.maxDimension(quality)));
        } catch (IOException | RuntimeException exception) {
            future.completeExceptionally(new ScreenshotException(exception));
        }
    }

    private static AiImageAttachment encode(NativeImage image, int maxDimension) throws IOException {
        Path source = Files.createTempFile("minemind-screenshot-", ".png");
        try {
            image.writeToFile(source);
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null) {
                throw new IOException("Screenshot image cannot be decoded");
            }

            ImageSize target = targetSize(original.getWidth(), original.getHeight(), maxDimension);
            BufferedImage output = original;
            if (target.width() != original.getWidth() || target.height() != original.getHeight()) {
                output = new BufferedImage(target.width(), target.height(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = output.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics.drawImage(original, 0, 0, target.width(), target.height(), null);
                } finally {
                    graphics.dispose();
                }
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(output, "png", bytes);
            return new AiImageAttachment(
                    "image/png",
                    Base64.getEncoder().encodeToString(bytes.toByteArray()),
                    target.width(),
                    target.height()
            );
        } finally {
            Files.deleteIfExists(source);
        }
    }

    static ImageSize targetSize(int width, int height, int maxDimension) {
        if (width <= 0 || height <= 0) {
            return new ImageSize(0, 0);
        }
        int max = Math.max(1, maxDimension);
        int largest = Math.max(width, height);
        if (largest <= max) {
            return new ImageSize(width, height);
        }
        double scale = (double) max / largest;
        return new ImageSize(Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale)));
    }

    static final class ScreenshotException extends RuntimeException {
        ScreenshotException() {
            super();
        }

        ScreenshotException(Throwable cause) {
            super(cause);
        }
    }

    record ImageSize(int width, int height) {
    }
}
