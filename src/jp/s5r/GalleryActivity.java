package jp.s5r;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GalleryActivity extends Activity {
    private static final int NUM_COLUMN = 4;

    private GristAdapter<GalleryItem> mAdapter;

    private int mImageSize;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 回転時の onDestroy を抑制
        super.onConfigurationChanged(newConfig);
    }

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
                null, null, null,
                String.format("%s DESC", MediaStore.Images.ImageColumns.DATE_TAKEN)
        );

        ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();

        c.moveToFirst();
        do {
            int idIndex = c.getColumnIndexOrThrow("_id");
            int takenIndex = c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN);

            GalleryItem item = new GalleryItem(
                    getApplicationContext(),
                    c.getLong(idIndex),
                    c.getString(takenIndex)
            );

            items.add(item);
        } while (c.moveToNext());

        mAdapter.buildItemsIndex(items);
    }

    private class MyGalleryAdapter extends GristAdapter<GalleryItem> {

        private ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor();
        private LinkedList<Holder> mWorkStack = new LinkedList<Holder>();
        private Handler mHandler = new Handler();

        public MyGalleryAdapter(int numColumn) {
            super(numColumn);

            mScheduler.scheduleAtFixedRate(
                    new BitmapLoadThread(), 0, 100, TimeUnit.MILLISECONDS
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

            mWorkStack.add(new Holder(imageView, item));

            return v;
        }

        private class BitmapLoadThread implements Runnable {
            @Override
            public void run() {
                while (true) {
                    Holder holder;
                    synchronized (GalleryActivity.this) {
                        holder = mWorkStack.poll();
                    }

                    if (holder == null) {
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
                    holder.imageView.setOnClickListener(new GalleryActivity.OnItemClickListener(holder.item));
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
    }

    class OnItemClickListener implements View.OnClickListener {
        private GalleryItem item;

        public OnItemClickListener(GalleryItem item) {
            this.item = item;
        }

        @Override
        public void onClick(View view) {
            onItemClick(item);
        }
    }

    private void onItemClick(GalleryItem item) {
        Toast.makeText(getApplicationContext(), item.getTakenAt().toLocaleString(), Toast.LENGTH_SHORT).show();
    }
}

