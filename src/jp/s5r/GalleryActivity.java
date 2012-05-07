package jp.s5r;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

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

        private class BitmapLoadTask extends AsyncTask<Void, Void, Bitmap> {
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
                imageView.setOnClickListener(new GalleryActivity.OnItemClickListener(item));
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

