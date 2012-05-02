
package jp.s5r;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.Date;

public abstract class GristActivity extends Activity {

    protected ArrayList<Container> mContainers = new ArrayList<Container>();

    private static final int NUM_COLUMN = 7;

    protected class Container {
        String title;
        ArrayList<GalleryItem> items;

        public Container(String title) {
            this.title = title;
            items = new ArrayList<GalleryItem>();
        }
    }

    protected class GalleryItem {
        Bitmap bitmap;
        Date   taken;

        public GalleryItem(Bitmap bitmap, String taken) {
            this.bitmap = bitmap;
            this.taken = new Date(Long.valueOf(taken));
        }
    }

    protected class GalleryRowAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public GalleryRowAdapter(Context context) {
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private int getRowSize(Container c) {
            return ((c.items.size() + NUM_COLUMN - 1) / NUM_COLUMN) + 1;
        }

        @Override
        public int getCount() {
            int count = 0;
            for (Container c : mContainers) {
                count += getRowSize(c);
            }

            return count;
        }

        @Override
        public ArrayList<GalleryItem> getItem(int position) {
            ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();

            int totalRowSize = 0;
            for (Container c : mContainers) {
                int rowSize = getRowSize(c);
                if (totalRowSize == position) {
                    return null;
                }

                if ((totalRowSize + rowSize) > position) {
                    int offset = (position - totalRowSize - 1) * NUM_COLUMN;

                    for (int itemPos = offset; itemPos < c.items.size(); itemPos++) {
                        items.add(c.items.get(itemPos));
                        if (items.size() >= NUM_COLUMN) {
                            break;
                        }
                    }
                    break;
                }

                totalRowSize += rowSize;
            }

            return items;
        }

        private String getSeparatorTitle(int position) {
            int totalRowSize = 0;
            for (Container c : mContainers) {
                if (totalRowSize == position) {
                    return c.title;
                }

                totalRowSize += getRowSize(c);
            }

            return "";
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryRowHolder holder;
            if (convertView == null || convertView.getTag() == null) {
                convertView = mInflater.inflate(R.layout.gallery_row, parent, false);
                holder = new GalleryRowHolder();
                holder.container = (LinearLayout) convertView.findViewById(R.id.gallery_row_container);
                convertView.setTag(holder);
            } else {
                holder = (GalleryRowHolder) convertView.getTag();
            }

            if (holder.container.getChildCount() > 0) {
                holder.container.removeAllViews();
            }

            ArrayList<GalleryItem> items = getItem(position);
            if (items == null) {
                String title = getSeparatorTitle(position);
                View v = createSeparator(title);
                if (v != null) {
                    holder.container.addView(v);
                }
            } else {
                for (GalleryItem item : items) {
                    View v = createItem(item);
                    if (v != null) {
                        holder.container.addView(v);
                    }
                }
            }

            return convertView;
        }

        private class GalleryRowHolder {
            LinearLayout container;
        }
    }

    protected abstract View createSeparator(String title);
    protected abstract View createItem(GalleryItem item);
}

