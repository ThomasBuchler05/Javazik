package view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Palette de couleurs et factory de composants stylés pour l'interface graphique.
 * Évite de dupliquer le code de style à travers toutes les vues.
 *
 * Palette : teal rgb(0, 122, 123) + blanc, inspiration health-tech / fintech.
 */
public final class Styles {

    // ==================== PALETTE ====================

    /** Teal principal : sidebar, boutons d'action, accents. */
    public static final Color TEAL           = new Color(0, 122, 123);
    /** Teal foncé : hover et enfoncé. */
    public static final Color TEAL_DARK      = new Color(0, 100, 101);
    /** Teal clair : bouton sidebar actif, survol léger. */
    public static final Color TEAL_LIGHT     = new Color(0, 145, 146);
    /** Teal très clair pour fond subtil. */
    public static final Color TEAL_SURFACE   = new Color(230, 245, 245);

    /** Blanc pur : fond principal de la zone de contenu. */
    public static final Color BG_MAIN        = Color.WHITE;
    /** Gris très clair : fond de cartes / sections. */
    public static final Color BG_ALT         = new Color(245, 247, 248);
    /** Bordures fines. */
    public static final Color BORDER         = new Color(229, 231, 235);

    /** Texte principal (quasi-noir). */
    public static final Color TEXT           = new Color(17, 24, 39);
    /** Texte secondaire (gris). */
    public static final Color TEXT_MUTED     = new Color(107, 114, 128);
    /** Texte sur fond teal. */
    public static final Color TEXT_ON_TEAL   = Color.WHITE;

    /** Rouge pour actions dangereuses (suppression, déconnexion). */
    public static final Color DANGER         = new Color(220, 38, 38);
    public static final Color DANGER_DARK    = new Color(185, 28, 28);

    /** Vert succès. */
    public static final Color SUCCESS        = new Color(22, 163, 74);
    public static final Color SUCCESS_SURFACE = new Color(240, 253, 244);

    /** Orange avertissement. */
    public static final Color WARNING        = new Color(217, 119, 6);
    public static final Color WARNING_SURFACE = new Color(255, 251, 235);

    // ==================== POLICES ====================

    private static final String FAMILY = chooseFontFamily();

    public static final Font FONT_TITLE      = new Font(FAMILY, Font.BOLD,  26);
    public static final Font FONT_SUBTITLE   = new Font(FAMILY, Font.BOLD,  18);
    public static final Font FONT_BODY       = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD  = new Font(FAMILY, Font.BOLD,  14);
    public static final Font FONT_SMALL      = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD  = new Font(FAMILY, Font.BOLD,  12);
    public static final Font FONT_BUTTON     = new Font(FAMILY, Font.BOLD,  14);
    public static final Font FONT_SIDEBAR    = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font FONT_LOGO       = new Font(FAMILY, Font.BOLD,  22);

    private static String chooseFontFamily() {
        String[] preferences = {"Segoe UI", "SF Pro Text", "Helvetica Neue", "Helvetica", "Arial"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String pref : preferences) {
            for (String font : available) {
                if (font.equalsIgnoreCase(pref)) return pref;
            }
        }
        return Font.SANS_SERIF;
    }

    // ==================== CONSTANTES LAYOUT ====================

    public static final int SIDEBAR_WIDTH    = 260;
    public static final int PADDING_LG       = 24;
    public static final int PADDING_MD       = 16;
    public static final int PADDING_SM       = 8;

    // ==================== FACTORIES ====================

    /** Bouton principal : fond teal, texte blanc. */
    public static JButton primaryButton(String text) {
        JButton b = baseButton(text);
        paintButton(b, TEAL, TEXT_ON_TEAL, TEAL_DARK);
        return b;
    }

    /** Bouton secondaire : bordure teal, fond blanc, texte teal. */
    public static JButton secondaryButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(BG_MAIN);
        b.setForeground(TEAL);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TEAL, 1, true),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        b.setContentAreaFilled(true);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) { b.setBackground(TEAL_SURFACE); }
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) { b.setBackground(BG_MAIN); }
            }
        });
        return b;
    }

    /** Bouton dangereux : fond rouge, texte blanc. Pour suppression/déconnexion. */
    public static JButton dangerButton(String text) {
        JButton b = baseButton(text);
        paintButton(b, DANGER, TEXT_ON_TEAL, DANGER_DARK);
        return b;
    }

    private static JButton baseButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }

    private static void paintButton(JButton b, Color bg, Color fg, Color hover) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(hover);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(bg);
            }
        });
    }

    /** Grand titre d'écran (ex. "Catalogue musical"). */
    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT);
        return l;
    }

    /** Sous-titre ou titre de section. */
    public static JLabel subtitleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SUBTITLE);
        l.setForeground(TEXT);
        return l;
    }

    /** Texte simple. */
    public static JLabel bodyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT);
        return l;
    }

    /** Texte atténué (indications, labels). */
    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    /** Panneau de type "carte" : fond alt, bordure douce, coins arrondis simulés par padding. */
    public static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG_ALT);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 219, 227), 1, true),
                BorderFactory.createEmptyBorder(PADDING_MD, PADDING_MD, PADDING_MD, PADDING_MD)
        ));
        return p;
    }

    /**
     * Bouton "lien" sans fond ni bordure : texte teal souligné au survol.
     * Utile pour les actions secondaires discrètes.
     */
    public static JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_SMALL);
        b.setForeground(TEAL);
        b.setBackground(null);
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setForeground(TEAL_DARK);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setForeground(TEAL);
            }
        });
        return b;
    }

    /** Séparateur horizontal fin pour la sidebar. */
    public static Border sidebarSeparator() {
        return BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 60));
    }

    /**
     * Badge ID : fond teal très clair, texte teal, coins arrondis simulés.
     * Utilisé pour afficher les IDs dans les listes admin.
     */
    public static JLabel badgeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL_BOLD);
        l.setForeground(TEAL);
        l.setBackground(TEAL_SURFACE);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 225, 225), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return l;
    }

    /**
     * Panneau "carte" avec bordure gauche colorée (accent bar).
     * Donne un aspect plus dynamique aux sections.
     */
    public static JPanel accentCardPanel(Color accentColor) {
        JPanel p = new JPanel();
        p.setBackground(BG_ALT);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(214, 219, 227), 1, false),
                        BorderFactory.createEmptyBorder(PADDING_MD, PADDING_MD, PADDING_MD, PADDING_MD)
                )
        ));
        return p;
    }

    private Styles() { /* utility class */ }
}