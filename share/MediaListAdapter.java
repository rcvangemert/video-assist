package com.vangemert.videoAssist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bitmapcache.BitmapLruCache;

import java.io.File;
import java.util.List;

public class MediaListAdapter extends BaseAdapter {
    private static LayoutInflater inflater = null;
    private final Context context;
    private List<Item> items;
    private BitmapLruCache cache;
    private final String graphicPath;
    private final String videoPath;

    static public class Item {
        String name;
        String url;
        long lastModified;
        boolean isFile;

        public Item(String name, boolean isFile, long lastModified, String url) {
            this.name = name;
            this.isFile = isFile;
            this.lastModified = lastModified;
            this.url = url;
        }
    }

    public MediaListAdapter(Context context, BitmapLruCache cache, List<Item> items) {
        this.context = context;
        this.cache = cache;
        this.items = items;

        if (cache == null) {
            BitmapLruCache.Builder builder = new BitmapLruCache.Builder(context);
            builder.setDiskCacheEnabled(true).setDiskCacheLocation(new File(context.getCacheDir().getAbsolutePath()));
            this.cache = builder.build();
        }

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        graphicPath = new Preferences(context).graphicsPath();
        videoPath = new Preferences(context).videoPath();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    public boolean isFile(int position) { return items.get(position).isFile; }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        String filename, key;

        View rowView = convertView;
        if (rowView == null)
            rowView = inflater.inflate(R.layout.list_item, null /*parent*/);

        if (((ListView)parent).isItemChecked(position))
            rowView.setBackgroundColor(context.getResources().getColor(R.color.selectedBackground));
        else
            rowView.setBackgroundColor(context.getResources().getColor(R.color.transparent));

        ImageView img = rowView.findViewById(R.id.image1);
        img.setImageBitmap(null);
        img.setTag(position);

        TextView tv1 = rowView.findViewById(R.id.text1);
        tv1.setText(items.get(position).name);

        TextView tv2 = rowView.findViewById(R.id.text2);
        tv2.setTag(position);

        if (items.get(position).url != null && items.get(position).url.contains("rtsp://")) {
            tv2.setText(items.get(position).url);
            tv2.setVisibility(View.VISIBLE);

        } else if (items.get(position).url != null && items.get(position).url.contains("juice://")) {
            tv2.setText(items.get(position).url);
            tv2.setVisibility(View.VISIBLE);

        } else {
            tv2.setVisibility(View.GONE);
            tv2.setText("");
        }

        if (items.get(position).name.endsWith(".pdf")) {
            if (cache != null) {
                filename = graphicPath + "/" + items.get(position).name;
                key = items.get(position).name + new File(filename).lastModified();
                new ImageViewCache(context, position, img, filename, cache, key).execute();
            }

        } else if (items.get(position).name.endsWith(".xml")) {
            if (cache != null) {
                filename = graphicPath + "/" + items.get(position).name;
                key = items.get(position).name + new File(filename).lastModified();
                new ImageViewCache(context, position, img, filename, cache, key).execute();
            }

        } else if (items.get(position).name.endsWith(".png") || items.get(position).name.endsWith(".jpg") || items.get(position).name.endsWith(".webp")) {
            if (cache != null) {
                filename = graphicPath + "/" + items.get(position).name;
                key = items.get(position).name + new File(filename).lastModified();
                new ImageViewCache(context, position, img, filename, cache, key).execute();
            }

        } else if (items.get(position).name.contains(".mp4")) {
            if (cache != null) {
                filename = videoPath + "/" + items.get(position).name;
                key = items.get(position).name + new File(filename).lastModified();
                new ImageViewCache(context, position, img, filename, cache, key).execute();
            }
        }

        return rowView;
    }

    public void updateItems(List<Item> newItems) {
        this.items = newItems; // Swap reference safely
        notifyDataSetChanged();
    }
}
