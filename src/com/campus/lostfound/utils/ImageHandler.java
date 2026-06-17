/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;

/**
 *
 * @author aksha
 */
public class ImageHandler {

    private static final String IMAGE_DIRECTORY = "uploaded_images";
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 600;

    static {
        File dir = new File(IMAGE_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdir();
            System.out.println("Created image directory: " + dir.getAbsolutePath());
        }
    }

    public static String saveImage(File sourceFile) {
        try {
            // Read the image first to validate it's actually an image
            BufferedImage originalImage = ImageIO.read(sourceFile);

            if (originalImage == null) {
                // If ImageIO can't read it, try copying as-is
                System.out.println("Warning: File might not be a valid image, copying as-is");
                return copyImageFile(sourceFile);
            }

            // Generate unique filename
            String extension = getFileExtension(sourceFile.getName());
            if (extension.isEmpty() || (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg"))) {
                extension = "png"; // Default to PNG
            }

            String newFileName = UUID.randomUUID().toString() + "." + extension;
            File destFile = new File(IMAGE_DIRECTORY, newFileName);

            // Resize if needed
            BufferedImage imageToSave = originalImage;
            if (originalImage.getWidth() > MAX_WIDTH || originalImage.getHeight() > MAX_HEIGHT) {
                double scale = Math.min(
                        (double) MAX_WIDTH / originalImage.getWidth(),
                        (double) MAX_HEIGHT / originalImage.getHeight()
                );

                int newWidth = (int) (originalImage.getWidth() * scale);
                int newHeight = (int) (originalImage.getHeight() * scale);

                imageToSave = resizeImage(originalImage, newWidth, newHeight);
            }

            // Save the image
            boolean saved = ImageIO.write(imageToSave, extension.equals("png") ? "PNG" : "JPEG", destFile);

            if (saved) {
                System.out.println("Image saved successfully: " + destFile.getAbsolutePath());
                return IMAGE_DIRECTORY + File.separator + newFileName;
            } else {
                System.err.println("Failed to save image");
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error saving image: " + e.getMessage());
            e.printStackTrace();

            // Fallback: try simple file copy
            return copyImageFile(sourceFile);
        }
    }

    private static String copyImageFile(File sourceFile) {
        try {
            String extension = getFileExtension(sourceFile.getName());
            String newFileName = UUID.randomUUID().toString() + "." + extension;
            Path sourcePath = sourceFile.toPath();
            Path destPath = Paths.get(IMAGE_DIRECTORY, newFileName);

            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Image copied: " + destPath.toString());

            return IMAGE_DIRECTORY + File.separator + newFileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill with white background (in case of transparency)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw the image
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();

        return resized;
    }

    public static ImageIcon loadImage(String relativePath, int width, int height) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }

        // Try multiple approaches to load the image
        // Approach 1: Direct file with multiple path formats
        File imageFile = null;

        // Try as-is
        imageFile = new File(relativePath);
        if (!imageFile.exists()) {
            // Try with working directory
            imageFile = new File(System.getProperty("user.dir"), relativePath);
        }

        if (!imageFile.exists()) {
            // Try with forward slashes
            String fixedPath = relativePath.replace("\\", "/");
            imageFile = new File(System.getProperty("user.dir"), fixedPath);
        }

        if (!imageFile.exists()) {
            System.err.println("Image file not found: " + imageFile.getAbsolutePath());
            return null;
        }

        System.out.println("Loading image from: " + imageFile.getAbsolutePath());

        // Approach 2: Read the file content and create image from bytes
        try {
            byte[] imageData = Files.readAllBytes(imageFile.toPath());
            ImageIcon originalIcon = new ImageIcon(imageData);

            if (originalIcon.getIconWidth() > 0) {
                // Successfully loaded, now scale it
                Image img = originalIcon.getImage();
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (IOException e) {
            System.err.println("Failed to read image bytes: " + e.getMessage());
        }

        // Approach 3: Use ImageIO
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img != null) {
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (IOException e) {
            System.err.println("ImageIO failed: " + e.getMessage());
        }

        // Approach 4: Use Toolkit
        try {
            Image img = Toolkit.getDefaultToolkit().getImage(imageFile.getAbsolutePath());
            ImageIcon icon = new ImageIcon(img);

            // Force load
            MediaTracker tracker = new MediaTracker(new JLabel());
            tracker.addImage(img, 0);
            tracker.waitForID(0);

            if (!tracker.isErrorID(0)) {
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.err.println("Toolkit approach failed: " + e.getMessage());
        }

        System.err.println("All image loading methods failed for: " + relativePath);
        return null;
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    // Test method to verify image loading
    public static void testImageLoading(String imagePath) {
        System.out.println("\n=== Testing Image Loading ===");
        System.out.println("Path: " + imagePath);

        File file = new File(System.getProperty("user.dir"), imagePath);
        System.out.println("Full path: " + file.getAbsolutePath());
        System.out.println("File exists: " + file.exists());
        System.out.println("File size: " + file.length() + " bytes");
        System.out.println("Can read: " + file.canRead());

        // Try to identify the file type
        try {
            byte[] header = new byte[8];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(header);
            }

            // Check PNG signature
            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                    && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                System.out.println("File type: PNG (valid header)");
            } // Check JPEG signature
            else if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8) {
                System.out.println("File type: JPEG (valid header)");
            } else {
                System.out.println("File type: Unknown or corrupted");
                System.out.print("Header bytes: ");
                for (byte b : header) {
                    System.out.printf("%02X ", b);
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.err.println("Error reading file header: " + e.getMessage());
        }

        // Try loading with ImageIO
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                System.out.println("ImageIO: SUCCESS - " + img.getWidth() + "x" + img.getHeight());
            } else {
                System.out.println("ImageIO: FAILED - returned null");
            }
        } catch (Exception e) {
            System.out.println("ImageIO: ERROR - " + e.getMessage());
        }
    }
}
