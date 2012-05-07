package jp.s5r;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GalleryActivity extends Activity {
    private static final int NUM_COLUMN = 4;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd");

    private ArrayList<GalleryItem>    mItems = new ArrayList<GalleryItem>();
    private GristAdapter<GalleryItem> mAdapter;

    private int mImageSize;

    class GalleryItem implements IGristItem {
        private Context mContext;
        private Long mMediaStoreId;
        private String mBitmapPath;
        private SoftReference<Bitmap> mBitmapReference;
        private Date mTakenAt;
        private int mType;

        private static final int TYPE_CONTENT_PROVIDER = 0;
        private static final int TYPE_FILE             = 1;

        public GalleryItem(Context context, long mediaStoreId, String taken) {
            mType         = TYPE_CONTENT_PROVIDER;
            mContext      = context;
            mMediaStoreId = mediaStoreId;
            mTakenAt      = new Date(Long.valueOf(taken));
        }

        public GalleryItem(Context context, String path, String taken) {
            mType       = TYPE_FILE;
            mContext    = context;
            mBitmapPath = path;
            mTakenAt    = new Date(Long.valueOf(taken));
        }

        public Bitmap getBitmap() {
            Bitmap bitmap = null;
            if (mBitmapReference != null) {
                bitmap = mBitmapReference.get();
            }

            if (bitmap == null) {
                switch (mType) {
                    case TYPE_CONTENT_PROVIDER:
                        bitmap = getBitmapFromContentResolver();
                        break;

                    case TYPE_FILE:
                        bitmap = getBitmapFromFile();
                        break;
                }

                mBitmapReference = new SoftReference<Bitmap>(bitmap);
            }

            return bitmap;
        }

        private Bitmap getBitmapFromContentResolver() {
            return MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    mMediaStoreId,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    null
            );
        }

        private Bitmap getBitmapFromFile() {
            return BitmapFactory.decodeFile(mBitmapPath);
        }

        public Date getTakenAt() {
            return mTakenAt;
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public String getTitle() {
            return sdf.format(getTakenAt());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
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

    class MyGalleryAdapter extends GristAdapter<GalleryItem> {
        public MyGalleryAdapter(int numColumn) {
            super(numColumn);
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

            new BitmapLoadTask(imageView, item).execute();

            return v;
        }
    }

    private void initData() {
        // managedQuery では close を呼ばないこと
        Cursor c = managedQuery(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, null, null,
                String.format("%s DESC", MediaStore.Images.ImageColumns.DATE_TAKEN)
        );
        c.moveToFirst();

        do {
            long id = c.getLong(c.getColumnIndexOrThrow("_id"));

            String taken = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN));

            GalleryItem item = new GalleryItem(getApplicationContext(), id, taken);

            mItems.add(item);

        } while (c.moveToNext());


        mAdapter.buildItemsIndex(mItems);
    }

    class BitmapLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView imageView;
        private GalleryItem item;

        public BitmapLoadTask(ImageView imageView, GalleryItem item) {
            this.imageView = imageView;
            this.item = item;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return item.getBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
            imageView.setOnClickListener(new OnItemClickListener(item));
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

