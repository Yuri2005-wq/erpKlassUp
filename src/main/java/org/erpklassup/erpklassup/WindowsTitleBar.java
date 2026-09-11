package org.erpklassup.erpklassup;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import javafx.stage.Stage;

/**
 * Classe utilitaire pour changer la couleur de la barre de titre Windows
 */
public class WindowsTitleBar {

    // Interface JNA pour Dwmapi.dll
    private interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        int DwmSetWindowAttribute(
                WinDef.HWND hwnd,
                int dwAttribute,
                byte[] pvAttribute,
                int cbAttribute
        );
    }

    // Constantes DWM
    private static final int DWMWA_BORDER_COLOR = 34;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;

    /**
     * Change la couleur de la barre de titre Windows
     * @param stage Le stage JavaFX
     * @param hexColor La couleur en hexadécimal (ex: "#16213A")
     */
    public static void setTitleBarColor(Stage stage, String hexColor) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        try {
            // ✅ Récupérer la fenêtre active au premier plan au lieu de chercher par le titre
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();

            if (hwnd == null || hwnd.getPointer() == null) {
                System.out.println("Impossible de trouver la fenêtre active");
                return;
            }

            // Convertir la couleur hex en RGB
            int[] rgb = hexToRgb(hexColor);
            int r = rgb[0];
            int g = rgb[1];
            int b = rgb[2];

            // Appliquer les couleurs
            applyCaptionColor(hwnd, r, g, b);
            applyBorderColor(hwnd, r, g, b);
            applyTextColor(hwnd, 255, 255, 255); // Texte blanc

        } catch (Exception e) {
            System.err.println("Erreur lors du changement de couleur : " + e.getMessage());
        }
    }

    public static void setCaptionColorOnly(Stage stage, String hexColor) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        try {
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();

            if (hwnd == null || hwnd.getPointer() == null) {
                return;
            }

            int[] rgb = hexToRgb(hexColor);
            applyCaptionColor(hwnd, rgb[0], rgb[1], rgb[2]);

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private static int[] hexToRgb(String hexColor) {
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new int[]{r, g, b};
    }

    private static void applyCaptionColor(WinDef.HWND hwnd, int r, int g, int b) {
        byte[] colorBytes = createColorBytes(r, g, b);
        Dwmapi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DWMWA_CAPTION_COLOR,
                colorBytes,
                colorBytes.length
        );
    }

    private static void applyBorderColor(WinDef.HWND hwnd, int r, int g, int b) {
        byte[] colorBytes = createColorBytes(r, g, b);
        Dwmapi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DWMWA_BORDER_COLOR,
                colorBytes,
                colorBytes.length
        );
    }

    private static void applyTextColor(WinDef.HWND hwnd, int r, int g, int b) {
        byte[] colorBytes = createColorBytes(r, g, b);
        Dwmapi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DWMWA_TEXT_COLOR,
                colorBytes,
                colorBytes.length
        );
    }

    private static byte[] createColorBytes(int r, int g, int b) {
        int colorValue = (b << 16) | (g << 8) | r;

        return new byte[] {
                (byte)(colorValue & 0xFF),
                (byte)((colorValue >> 8) & 0xFF),
                (byte)((colorValue >> 16) & 0xFF),
                (byte)((colorValue >> 24) & 0xFF)
        };
    }
}
//package org.erpklassup.erpklassup;
//
//import com.sun.jna.Native;
//import com.sun.jna.platform.win32.User32;
//import com.sun.jna.platform.win32.WinDef;
//import com.sun.jna.win32.StdCallLibrary;
//import javafx.stage.Stage;
//
///**
// * Classe utilitaire pour changer la couleur de la barre de titre Windows
// */
//public class WindowsTitleBar {
//
//    // Interface JNA pour Dwmapi.dll
//    private interface Dwmapi extends StdCallLibrary {
//        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);
//
//        int DwmSetWindowAttribute(
//                WinDef.HWND hwnd,
//                int dwAttribute,
//                byte[] pvAttribute,
//                int cbAttribute
//        );
//    }
//
//    // Constantes DWM
//    private static final int DWMWA_BORDER_COLOR = 34;
//    private static final int DWMWA_CAPTION_COLOR = 35;
//    private static final int DWMWA_TEXT_COLOR = 36;
//
//    /**
//     * Change la couleur de la barre de titre Windows
//     * @param stage Le stage JavaFX
//     * @param hexColor La couleur en hexadécimal (ex: "#16213A")
//     */
//    public static void setTitleBarColor(Stage stage, String hexColor) {
//        // Vérifier si on est sur Windows
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            System.out.println("Cette fonctionnalité ne fonctionne que sur Windows");
//            return;
//        }
//
//        try {
//            // Obtenir le handle de la fenêtre Windows
//            WinDef.HWND hwnd = new WinDef.HWND();
//            hwnd.setPointer(User32.INSTANCE.FindWindow(null, stage.getTitle()).getPointer());
//
//            if (hwnd.getPointer() == null) {
//                System.out.println("Impossible de trouver la fenêtre");
//                return;
//            }
//
//            // Convertir la couleur hex en RGB
//            int[] rgb = hexToRgb(hexColor);
//            int r = rgb[0];
//            int g = rgb[1];
//            int b = rgb[2];
//
//            // Appliquer les couleurs
//            applyCaptionColor(hwnd, r, g, b);
//            applyBorderColor(hwnd, r, g, b);
//            applyTextColor(hwnd, 255, 255, 255); // Texte blanc
//
//            System.out.println("Couleur de la barre de titre changée avec succès");
//
//        } catch (Exception e) {
//            System.err.println("Erreur lors du changement de couleur : " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Change uniquement la couleur de la barre de titre (sans la bordure)
//     */
//    public static void setCaptionColorOnly(Stage stage, String hexColor) {
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            return;
//        }
//
//        try {
//            WinDef.HWND hwnd = new WinDef.HWND();
//            hwnd.setPointer(User32.INSTANCE.FindWindow(null, stage.getTitle()).getPointer());
//
//            if (hwnd.getPointer() == null) {
//                return;
//            }
//
//            int[] rgb = hexToRgb(hexColor);
//            applyCaptionColor(hwnd, rgb[0], rgb[1], rgb[2]);
//
//        } catch (Exception e) {
//            System.err.println("Erreur : " + e.getMessage());
//        }
//    }
//
//    /**
//     * Convertit une couleur hexadécimale en RGB
//     */
//    private static int[] hexToRgb(String hexColor) {
//        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
//
//        int r = Integer.parseInt(hex.substring(0, 2), 16);
//        int g = Integer.parseInt(hex.substring(2, 4), 16);
//        int b = Integer.parseInt(hex.substring(4, 6), 16);
//
//        return new int[]{r, g, b};
//    }
//
//    /**
//     * Applique la couleur à la barre de titre
//     */
//    private static void applyCaptionColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_CAPTION_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    /**
//     * Applique la couleur à la bordure
//     */
//    private static void applyBorderColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_BORDER_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    /**
//     * Applique la couleur au texte
//     */
//    private static void applyTextColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_TEXT_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    /**
//     * Crée les bytes de couleur au format Windows (0x00BBGGRR)
//     */
//    private static byte[] createColorBytes(int r, int g, int b) {
//        int colorValue = (b << 16) | (g << 8) | r;
//
//        return new byte[] {
//                (byte)(colorValue & 0xFF),           // Rouge
//                (byte)((colorValue >> 8) & 0xFF),    // Vert
//                (byte)((colorValue >> 16) & 0xFF),   // Bleu
//                (byte)((colorValue >> 24) & 0xFF)    // Alpha (0 = opaque)
//        };
//    }
//}