package cn.edu.buaa.wangye.photosave;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
    private static final String PUT_HERE_FILE_NAME_TO_STORE_IMAGE = "image.jpg";
    TextView textView;
    ImageView imageView;
    ProgressDialog progressDialog;
    private ShareActionProvider mShareActionProvider;
    Bitmap bitmap;
    Uri loaclImageUri;
    int progressdata;
    String filename;
    String uriFilename;
    int totalPics = 1;
    int curPic = 1;
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 0:
                    Toast.makeText(MainActivity.this, "已保存到/Pictures/PhotoPlusSave",Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case 1:
                    textView.setText("图片资源获取中("+curPic+"/"+totalPics+")...("+progressdata/1024+"kb)");
                    break;
                case 2:
                    Toast.makeText(MainActivity.this, "不支持的应用！",Toast.LENGTH_LONG).show();
                    finish();
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        //imageView = (ImageView) findViewById(R.id.image);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            } else if (type.startsWith("application/")) {
                handleSendApplication(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_ATTACH_DATA.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleAttachImage(intent); // Handle multiple images
                // being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images
                // being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }
    }

    private void handleAttachImage(Intent intent) {
        final Uri imageUri = (Uri) intent.getData();
        ArrayList<Uri> imageUris = new ArrayList<>();
        imageUris.add(imageUri);
        downloadPicWithUri(imageUris);

    }

    private void handleSendApplication(Intent intent){
        Uri imageUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
        System.out.println(imageUri.toString());
        ArrayList<Uri> imageUris = new ArrayList<>();
        imageUris.add(imageUri);
        downloadPicWithUri(imageUris);

    }

    public void downloadPicWithUri(final ArrayList<Uri> imageUris) {
        if (imageUris != null){
            new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    ContentResolver cr = getContentResolver();
                    totalPics = imageUris.size();
                    for (int i = 0; i < totalPics; i++){
                        Uri imageUri = imageUris.get(i);
                        curPic = i+1;
                        //filename = md5(imageUri.toString());
                        filename = genFileName();
                        System.out.println(imageUri.toString());
                        try {
                            InputStream is = cr.openInputStream(imageUri);
                            byte[] isbyte = convertInputStreamToByteArray(is);

                            Movie gif = Movie.decodeByteArray(isbyte, 0, isbyte.length);
                            BufferedOutputStream bos;
                            String sd = Environment.getExternalStorageDirectory().getPath();
                            String photodir = sd+"/Pictures/PhotoPlusSave/";
                            File photodirFile = new File(photodir);
                            photodirFile.mkdirs();

                            if (gif != null) {
                                System.out.println("gif");
                                uriFilename = photodir+filename+".gif";
                                bos = new BufferedOutputStream(new FileOutputStream(uriFilename));
                            } else {
                                System.out.println("jpeg");
                                uriFilename = photodir+filename+".jpg";
                                bos = new BufferedOutputStream(new FileOutputStream(uriFilename));

                            }
                            try {
                                bos.write(isbyte);
                                bos.flush();
                                bos.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.parse("file://"+uriFilename)));
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    handler.sendEmptyMessage(0);
                }
            }).start();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Return true to display menu
        return true;

    }

    // Call to update the share intent



    private void handleSendText(Intent intent) {
        // TODO Auto-generated method stub
        final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        System.out.println(sharedText);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        String gag = sharedText.substring(sharedText.indexOf("http://"));
                        //filename = md5(gag);
                        filename = genFileName();
                        String pic = "";

                        Document doc = Jsoup.connect(gag).get();
                        Elements elements = doc.getElementsByClass("badge-item-img");
                        for (Element element : elements) {
                            //System.out.println(element.attr("src"));
                            pic = element.attr("src");
                        }
                        elements = doc.getElementsByClass("badge-item-animated-img");
                        for (Element element : elements) {
                            pic = element.attr("src");
                            //System.out.println(element.attr("src"));
                        }
                        if(!(pic.endsWith(".gif")||pic.endsWith(".jpg")||pic.endsWith(".jpeg")||pic.endsWith(".bmp"))){
                            handler.sendEmptyMessage(2);
                            return;
                        }
                        try {
                            URL url = new URL(pic);
                            HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();

                            InputStream is = urlConn.getInputStream();
                            byte[] isbyte = convertInputStreamToByteArray(is);

                            BufferedOutputStream bos;
                            String sd = Environment.getExternalStorageDirectory().getPath();
                            String photodir = sd+"/Pictures/PhotoPlusSave/";
                            File photodirFile = new File(photodir);
                            photodirFile.mkdirs();


                            uriFilename = photodir+filename+pic.substring(pic.lastIndexOf("."));
                            bos = new BufferedOutputStream(new FileOutputStream(uriFilename));

                            try {
                                bos.write(isbyte);
                                bos.flush();
                                bos.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            //bitmap = BitmapFactory.decodeByteArray(isbyte, 0, isbyte.length);
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        handler.sendEmptyMessage(2);
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(0);

                }
            }).start();


        }
    }


    void handleSendImage(Intent intent) {
        final Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        ArrayList<Uri> imageUris = new ArrayList<>();
        imageUris.add(imageUri);
        downloadPicWithUri(imageUris);
    }

    public byte[] convertInputStreamToByteArray(InputStream inputStream) {
        byte[] bytes = null;
        progressdata = 0;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte data[] = new byte[1024];
            int count;

            while ((count = inputStream.read(data)) != -1) {
                bos.write(data, 0, count);
                progressdata += count;
                handler.sendEmptyMessage(1);

            }

            bos.flush();
            bos.close();
            inputStream.close();

            bytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
    void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent
                .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
            downloadPicWithUri(imageUris);
        }
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {

		/*
		  ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		  inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		  String path = Images.Media.insertImage(inContext.getContentResolver(), inImage, "PhotoPlusShare", "WebPhotoForShareing");
		  return Uri.parse(path);

		*/

        String sd = Environment.getExternalStorageDirectory().getPath();
        String photodir = sd+"/Pictures/PhotoPlusSave/";
        File photodirFile = new File(photodir);
        photodirFile.mkdirs();
        File dest = new File(photodirFile, filename+".jpg");

        try {
            FileOutputStream out = new FileOutputStream(dest);
            inImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.fromFile(dest);


    }


    public static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String genFileName(){
        Date date = new Date();
        String filename = "PhotoPlusSave_"+(new java.text.SimpleDateFormat(
                "yyyy-MM-dd-hh-mm-ss_")).format(date)+date.getTime();
        return filename;
    }
}
