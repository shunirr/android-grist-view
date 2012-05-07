package jp.s5r;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GalleryItem implements IGristItem {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd");

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

