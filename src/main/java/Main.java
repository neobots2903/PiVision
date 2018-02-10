import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class Main {
  private static int centerX;
private static int width;

public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");
    // Connect NetworkTables, and get access to the publishing table
    NetworkTable.setClientMode();
    // Set your team number here
    NetworkTable.setTeam(2903);

    NetworkTable.initialize();

    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;

    // This stores our reference to our mjpeg server for streaming the input image
    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);

    /***********************************************/
    // USB Camera
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = setUsbCamera(0, inputStream);
    
    // Set the resolution for our camera, since this is over USB
    camera.setResolution(640,480);
    
    // This creates a CvSink for us to use. This grabs images from our selected camera, 
    // and will allow us to use those images in opencv
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    // This creates a CvSource to use. This will take in a Mat image that has had OpenCV operations
    // operations 
    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);

    // All Mats and Lists should be stored outside the loop to avoid allocations
    // as they are expensive to create
    Mat inputImage = new Mat();
    GripPipeline pipeline = new GripPipeline();
    
    // Infinitely process image
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // Just skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;
      
      // Call the grip pipeline to process the image
      pipeline.process(inputImage);
      
      if (!pipeline.filterContoursOutput().isEmpty()) {
			Rect r = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
			centerX = r.x + (r.width / 2);
			width = r.width;
      }
      
      // Send centerX and width back to Roborio
      NetworkTable.getTable("SmartDashboard").setDefaultNumber("centerX", centerX);
      NetworkTable.getTable("SmartDashboard").setDefaultNumber("width", width);
    }
  }

  private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
    server.setSource(camera);
    return camera;
  }
}