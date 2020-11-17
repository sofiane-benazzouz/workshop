package epsi.project.workshop;

import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;
 

public class App implements VideoDisplayListener<MBFImage> {
 
    /** Le d�tecteur de visages. */
    private FaceDetector<DetectedFace,FImage> detecteurVisages;
 
    /** Constantes et propri�t�s n�cessaires � l'affichage de la zone de tracking. */
    private static int LARGEUR_WEBCAM = 640;
    private static int HAUTEUR_WEBCAM = 480;
 
    private static int LARGEUR_ZONE_TRACKING = 180;
    private static int HAUTEUR_ZONE_TRACKING = 180;
 
    private static int X1 = (LARGEUR_WEBCAM - LARGEUR_ZONE_TRACKING) / 2;
    private static int X2 = X1 + LARGEUR_ZONE_TRACKING;
    private static int Y1 = (HAUTEUR_WEBCAM - HAUTEUR_ZONE_TRACKING) / 2;
    private static int Y2 = Y1 + HAUTEUR_ZONE_TRACKING;
 
    private Rectangle zoneTracking;
 
    private Polygon flecheHaut;
    private Polygon flecheBas;
    private Polygon flecheGauche;
    private Polygon flecheDroite;
 
    /** Contructeur. */
    public App() throws Exception {
 
        // Initialisation de la zone de tracking et des diff�rents �l�ments � afficher
        zoneTracking = new Rectangle(X1, Y1, LARGEUR_ZONE_TRACKING, HAUTEUR_ZONE_TRACKING);
        flecheBas = new Polygon(new Point2dImpl(0, 30), new Point2dImpl(-10, 10), new Point2dImpl(-5, 10), new Point2dImpl(-5, -30), new Point2dImpl(5, -30), new Point2dImpl(5, 10), new Point2dImpl(10, 10));
        flecheHaut = flecheBas.clone();
        flecheHaut.rotate(Math.PI);
        flecheGauche = flecheBas.clone();
        flecheGauche.rotate(Math.PI / 2);
        flecheDroite = flecheBas.clone();
        flecheDroite.rotate(-Math.PI / 2);
 
        flecheBas.translate(LARGEUR_WEBCAM / 2, Y2 + Y1 / 2);
        flecheHaut.translate(LARGEUR_WEBCAM / 2, Y1 / 2);
        flecheGauche.translate(X1 / 2, HAUTEUR_WEBCAM / 2);
        flecheDroite.translate(X2 + X1 / 2, HAUTEUR_WEBCAM / 2);
 
        // Initialisation du flux de capture sur la webcam
        final VideoCapture capture = new VideoCapture(LARGEUR_WEBCAM, HAUTEUR_WEBCAM);
 
        // Cr�ation d'un affichage du flux vid�o
        final VideoDisplay<MBFImage> videoFrame = VideoDisplay.createVideoDisplay(capture);
 
        // Ajout de l'�couteur sur le flux vid�o
        videoFrame.addVideoListener(this);
 
        // Cr�ation du d�tecteur de visages (d�tecteur de Haar avec une taille minimum des visages de 80 px, bonne valeur pour la r�solution de webcam utilis�e et pour des calculs rapides)
        detecteurVisages = new HaarCascadeDetector(80);
 
    }
 
    
    /** Impl�mentation du listener (apr�s affichage de l'image dans le flux vid�o) ==> rien � faire. */
    public void afterUpdate(VideoDisplay<MBFImage> display) {
    }
     
    
    /** Impl�mentation du listener (avant affichage de l'image dans le flux vid�o) ==> d�tection des visages et affichage des zones d�tect�es. */
    public synchronized void beforeUpdate(MBFImage frame) {
 
        // Recherche du visage le plus grand (celui dont le cadre a l'aire la plus grande)
        double aireVisagePlusGrand = 0d;
        DetectedFace visagePlusGrand = null;
        final List<DetectedFace> listeVisagesDetectes = detecteurVisages.detectFaces(Transforms.calculateIntensity(frame));
        for (final DetectedFace visageDetecte : listeVisagesDetectes) {
            // Cadre autour des visages d�tect�s
            frame.drawShape(visageDetecte.getShape(), 3, RGBColour.ORANGE);
            final double aireVisage = visageDetecte.getBounds().calculateArea();
            if (aireVisage > aireVisagePlusGrand) {
                aireVisagePlusGrand = aireVisage;
                visagePlusGrand = visageDetecte;
            }
        }
 
        if (visagePlusGrand != null) {
            // R�cup�ration du centre de gravit� du visage
            final Point2d centreVisage = visagePlusGrand.getBounds().calculateRegularBoundingBox().getTopLeft();
            // Affichage du centre de gravit� du visage
            frame.drawPoint(centreVisage, RGBColour.BLUE, 15);
 
            // Couleur diff�rente de la zone de tracking selon si le centre de gravit� est � l'int�rieur de la zone ou pas
            if (zoneTracking.isInside(centreVisage)) {
                frame.drawShape(zoneTracking, 5, RGBColour.GREEN);
            } else {
                frame.drawShape(zoneTracking, 5, RGBColour.RED);
            }
 
            // S'il faut tourner horizontalement
            if (centreVisage.getX() < X1 || centreVisage.getX() > X2) {
                if (centreVisage.getX() < X1) {
                    // Si le centre du visage est � gauche de la zone de tracking : tourner � gauche
                    frame.drawShape(flecheGauche, 3, RGBColour.YELLOW);
                } else {
                    // Si le centre du visage est � droite de la zone de tracking : tourner � droite
                    frame.drawShape(flecheDroite, 3, RGBColour.YELLOW);
                }
            } else {
                // Le centre du visage est dans la zone de tracking : on stoppe
            }
 
            // S'il faut tourner verticalement
            if (centreVisage.getY() < Y1 || centreVisage.getY() > Y2) {
                if (centreVisage.getY() < Y1) {
                    // Si le centre du visage est au-dessus de la zone de tracking : tourner vers le haut
                    frame.drawShape(flecheHaut, 3, RGBColour.YELLOW);
                } else {
                    // Si le centre du visage est en-dessous de la zone de tracking : tourner vers le bas
                    frame.drawShape(flecheBas, 3, RGBColour.YELLOW);
                }
            } else {
                // Le centre du visage est dans la zone de tracking : on stoppe
            }
 
        } else {
            frame.drawShape(zoneTracking, 5, RGBColour.BLUE);
            // Pas de visage : on stoppe tout
        }
    }
 
    public static void main(String[] args) throws Exception {
        new App();
    }
 
}