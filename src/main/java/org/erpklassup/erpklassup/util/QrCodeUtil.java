package org.erpklassup.erpklassup.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

public class QrCodeUtil {

    public static Image genererQrCodeImage(String contenuTextuel, int largeur, int hauteur) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(contenuTextuel, BarcodeFormat.QR_CODE, largeur, hauteur);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            System.err.println("❌ Erreur de génération du QR Code : " + e.getMessage());
            return null;
        }
    }
}