package jp.s5r;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public abstract class GristAdapter<T extends IGristItem> extends BaseAdapter {
    private int                  mNumColumns;
    private ArrayList<Container> mContainers;

    protected abstract View createTitleView(String title);
    protected abstract View createItemView(T item);

    protected abstract View getRowLayout(ViewGroup parent);
    protected abstract View getRowContainer(View convertView);

    private class Container {
        String title;
        ArrayList<T> items;

        public Container(String title) {
            this.title = title;
            this.items = new ArrayList<T>();
        }
    }

    public GristAdapter(int numColumns) {
        mNumColumns = numColumns;
        mContainers = new ArrayList<Container>();
    }

    public void buildItemsIndex(List<T> items) {
        for (T item : items) {
            if (mContainers.size() == 0) {
                mContainers.add(new Container(item.getTitle()));
            }

            Container container = mContainers.get(mContainers.size() - 1);
            if (container.title.equals(item.getTitle())) {
                // nop
            } else {
                mContainers.add(new Container(item.getTitle()));
                container = mContainers.get(mContainers.size() - 1);
            }

            container.items.add(item);
        }
    }

    private int getRowSize(Container c) {
        return ((c.items.size() + mNumColumns - 1) / mNumColumns) + 1;
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
    public List<T> getItem(int position) {
        ArrayList<T> items = new ArrayList<T>();

        int totalRowSize = 0;
        for (Container c : mContainers) {
            int rowSize = getRowSize(c);
            if (totalRowSize == position) {
                return null;
            }

            if ((totalRowSize + rowSize) > position) {
                int offset = (position - totalRowSize - 1) * mNumColumns;

                for (int itemPos = offset; itemPos < c.items.size(); itemPos++) {
                    items.add(c.items.get(itemPos));
                    if (items.size() >= mNumColumns) {
                        break;
                    }
                }
                break;
            }

            totalRowSize += rowSize;
        }

        return items;
    }

    private String getTitle(int position) {
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
        RowHolder holder;
        if (convertView == null || convertView.getTag() == null) {
            convertView = getRowLayout(parent);

            holder = new RowHolder();
            holder.container = (LinearLayout) getRowContainer(convertView);

            convertView.setTag(holder);
        } else {
            holder = (RowHolder) convertView.getTag();
        }

        if (holder.container.getChildCount() > 0) {
            holder.container.removeAllViews();
        }

        List<T> items = getItem(position);
        if (items == null) {
            String title = getTitle(position);
            View v = createTitleView(title);
            if (v != null) {
                holder.container.addView(v);
            }
        } else {
            for (T item : items) {
                View v = createItemView(item);
                if (v != null) {
                    holder.container.addView(v);
                }
            }
        }

        return convertView;
    }

    private class RowHolder {
        LinearLayout container;
    }
}