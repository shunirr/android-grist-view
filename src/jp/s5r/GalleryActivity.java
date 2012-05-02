package jp.s5r;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

public class GalleryActivity extends GristActivity {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        initData();

        ListView galleryList = (ListView) findViewById(R.id.gallery_list);
        galleryList.setAdapter(
                new GalleryRowAdapter(getApplicationContext())
        );
    }

    private void initData() {
        Cursor c = null;
        try {
            c = managedQuery(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, null, null,
                    String.format("%s DESC", MediaStore.Images.ImageColumns.DATE_TAKEN)
            );
            c.moveToFirst();

            do {
                long id = c.getLong(c.getColumnIndexOrThrow("_id"));
                String taken = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN));
                Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                        getContentResolver(), id, MediaStore.Images.Thumbnails.MICRO_KIND, null);

                GalleryItem item = new GalleryItem(bitmap, taken);
                String title = sdf.format(item.taken);

                if (mContainers.size() == 0) {
                    mContainers.add(new Container(title));
                }

                Container container = mContainers.get(mContainers.size() - 1);
                if (container.title.equals(title)) {
                    // nop
                } else {
                    mContainers.add(new Container(title));
                    container = mContainers.get(mContainers.size() - 1);
                }

                container.items.add(item);

            } while (c.moveToNext());

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    protected View createSeparator(String title) {
        View v = getLayoutInflater().inflate(R.layout.gallery_item_category, null, false);
        TextView textView = (TextView) v.findViewById(R.id.gallery_item_text);

        textView.setText(title);

        return v;
    }

    @Override
    protected View createItem(GalleryItem item) {
        View v = getLayoutInflater().inflate(R.layout.gallery_item, null, false);
        ImageView imageView = (ImageView) v.findViewById(R.id.gallery_item_image);

        imageView.setImageBitmap(item.bitmap);
        imageView.setOnClickListener(new OnItemClickListener(item));

        return v;
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

    protected void onItemClick(GalleryItem item) {
        Toast.makeText(getApplicationContext(), item.taken.toLocaleString(), Toast.LENGTH_SHORT).show();
    }
}
