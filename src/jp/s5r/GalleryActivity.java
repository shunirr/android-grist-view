package jp.s5r;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GalleryActivity extends Activity {
    private static final int NUM_COLUMN = 4;

    private GristAdapter<GalleryItem> mAdapter;

    private int mImageSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        mImageSize = getWindowManager().getDefaultDisplay().getWidth() / NUM_COLUMN;

        mAdapter = new MyGalleryAdapter(NUM_COLUMN);

        ListView galleryList = (ListView) findViewById(R.id.gallery_list);
        galleryList.setAdapter(mAdapter);

        initData();
    }

    private void initData() {
        // managedQuery では close を呼ばないこと
        Cursor c = managedQuery(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, null, null,MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        ;

        ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();

        c.moveToFirst();
        do {
            int idIndex = c.getColumnIndexOrThrow("_id");
            int takenIndex = c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN);
            String taken = c.getString(takenIndex);
            
            if (taken != null) {
	            GalleryItem item = new GalleryItem(
	            		getApplicationContext(),
	                    c.getLong(idIndex),
	                    c.getString(takenIndex)
	                   
	            );
	            items.add(item);
            }

            
        } while (c.moveToNext());

        mAdapter.buildItemsIndex(items);
    }

    private void onItemClick(GalleryItem item) {
        Toast.makeText(getApplicationContext(), item.getTakenAt().toLocaleString(), Toast.LENGTH_SHORT).show();
    }


    private class MyGalleryAdapter extends GristAdapter<GalleryItem> {

        private ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor();
        private LinkedList<Holder> mWorkStack = new LinkedList<Holder>();
        private Handler mHandler = new Handler();
        private ScheduledFuture<?> msf;
        private BitmapLoadThread mThread;

        public MyGalleryAdapter(int numColumn) {
            super(numColumn);
            mThread =  new BitmapLoadThread();
            msf =  mScheduler.scheduleAtFixedRate(
                   mThread, 0, 100, TimeUnit.MILLISECONDS
            );
        }

        @Override
        protected View getRowLayout(ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.gallery_row, parent, false);
        }

        @Override
        protected View getRowContainer(View convertView) {
            return convertView.findViewById(R.id.gallery_row_container);
        }

        @Override
        protected View createTitleView(String title) {
            View v = getLayoutInflater().inflate(R.layout.gallery_item_category, null, false);
            TextView textView = (TextView) v.findViewById(R.id.gallery_item_text);

            textView.setText(title);

            return v;
        }

        @Override
        protected View createItemView(GalleryItem item) {
            View v = getLayoutInflater().inflate(R.layout.gallery_item, null, false);
            ImageView imageView = (ImageView) v.findViewById(R.id.gallery_item_image);
            imageView.setLayoutParams(new AbsListView.LayoutParams(mImageSize, mImageSize));

            Bitmap bitmap = item.getBitmapCache();
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setOnClickListener(new OnItemClickListener(item));
            } else {
                // Load async
                mWorkStack.add(new Holder(imageView, item));
                if (msf.isCancelled()) {
                	msf =  mScheduler.scheduleAtFixedRate(
                        mThread, 0, 100, TimeUnit.MILLISECONDS);
                }
            }

            return v;
        }

        private class BitmapLoadThread implements Runnable {
        	public  final String TAG = BitmapLoadThread.class.getName();
            @Override
            public void run() {
            	Log.i(TAG,"BitmapLoadThread run");
                while (true) {
                    Holder holder;
                    synchronized (GalleryActivity.this) {
                        holder = mWorkStack.poll();
                    }

                    if (holder == null) {
                    	msf.cancel(true);
                        break;
                    }

                    setImageAtUIThread(holder, holder.item.getBitmap());
                }
            }
        }

        private void setImageAtUIThread(final Holder holder, final Bitmap bitmap) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    holder.imageView.setImageBitmap(bitmap);
                    holder.imageView.setOnClickListener(new OnItemClickListener(holder.item));
                }
            });
        }

        public class Holder {
            ImageView imageView;
            GalleryItem item;

            public Holder(ImageView imageView, GalleryItem item) {
                this.imageView = imageView;
                this.item = item;
            }
        }

        class OnItemClickListener implements View.OnClickListener {
            private GalleryItem item;

            public OnItemClickListener(GalleryItem item) {
                this.item = item;
            }

            @Override
            public void onClick(View view) {
                GalleryActivity.this.onItemClick(item);
            }
        }
    }
}

