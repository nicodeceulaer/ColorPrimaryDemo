/***
  Copyright (c) 2008-2012 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain	a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
	
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
*/

package com.commonsware.android.fancylists.three;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class DynamicDemo extends ListActivity {
    private static final String TAG = "colorlist";
    private static final String[] items={"com.android.settings",
            "com.google.earth"};
    private static final int BAD_COLOR = 0xdeadbeef;
    AppAdapter adapter=null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // get a list of all launchable applications
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> launchables = pm.queryIntentActivities(main, 0);
        Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(pm));

        adapter=new AppAdapter(pm, launchables);
        setListAdapter(adapter);
    }
  
    class AppAdapter extends ArrayAdapter<ResolveInfo> {
        private PackageManager pm = null;

        AppAdapter( PackageManager pm, List<ResolveInfo> apps) {
            super(DynamicDemo.this, R.layout.row, R.id.label, apps);
            this.pm = pm;
        }

        /* look up the entity that will resolve the Launch intent of the requested package
        * we need to start there, since an application can have different primaryColor's for different
        * parts of the application.
        */
        private ResolveInfo ResolvePackage( String packageName ) {
            Intent intent = new Intent();
            intent.setPackage( packageName );
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, 0);

            if( resolveInfo == null ) {
                Log.i(TAG, "package not found for " + packageName );
            } else {
                Log.i(TAG, "found package " + resolveInfo.activityInfo.packageName);
            }

            return resolveInfo;
        }

        /* access R.styleable from outside of the platform, since Google threw this out of the SDK
        */
        public final <T> T getFieldFromStyleable(Context context, String name) {
          try {
              // use reflection to access the resource class
              // context.getPackageName() +
              Field field = Class.forName( "android.R$styleable" ).getField( name );
              if ( null != field ) {
                  return (T) field.get( null );
              }
          } catch ( Throwable t ) {
              t.printStackTrace();
          }
          return null;
        }

        private int GetColorFromPackage( Context localContext, String packageName ) {
            ResolveInfo resolveInfo = ResolvePackage( packageName );
            if( resolveInfo == null) { return BAD_COLOR; }

          try
          {
              Context         remoteCtx   = createPackageContext( packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
              Resources.Theme remoteTheme = remoteCtx.getTheme();

              remoteTheme.applyStyle( resolveInfo.activityInfo.getThemeResource(), true );
              int [] theme_data = getFieldFromStyleable( remoteCtx, "Theme" );

            /* R.styleable.Theme */
              TypedArray localTypedArray = remoteTheme.obtainStyledAttributes( theme_data );
              int color = localTypedArray.getColor(230, 0);
              localTypedArray.recycle();

              final Resources res = remoteCtx.getResources();
              int             id  = res.getIdentifier("colorPrimary", "color", packageName);

              Log.i(TAG, "method 1 gives color: " + Integer.toHexString( color ));
              int colorPrimaryId = getFieldFromStyleable( remoteCtx, "Theme_colorPrimary" );

              if( colorPrimaryId != 230 ){
                  Log.i(TAG, "the offset 230 should be = " + colorPrimaryId );
                  Log.i(TAG, "wrong colorPrimaryId for package " + packageName );
                  return BAD_COLOR;
              }

              int [] theme_id_list = { android.R.attr.colorPrimary };
              localTypedArray = remoteTheme.obtainStyledAttributes( theme_id_list );
              int color2 = localTypedArray.getColor(0, 0);
              localTypedArray.recycle();
              Log.i(TAG, "method 2 gives color: " + Integer.toHexString( color2 ));

              if( color != color2 ) {
                  Log.i(TAG, "found color difference for package " + packageName);
                  return BAD_COLOR;
              }

              return color;
          }
          catch (PackageManager.NameNotFoundException localNameNotFoundException)
          {
              localNameNotFoundException.printStackTrace();
          }

          Log.i(TAG, "took backup color : " + BAD_COLOR );
          return BAD_COLOR;
        }

        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent) {
            ResolveInfo ri = getItem( position );
            String packageName = ri.activityInfo.packageName;
            int color = GetColorFromPackage( getContext(), packageName);

            View row=super.getView(position, convertView, parent);
            View sample=(View)row.findViewById(R.id.sample);
            sample.setBackgroundColor( color );

            TextView txt_packagename = (TextView) row.findViewById(R.id.label );
            txt_packagename.setText(packageName);

            TextView txt_color = (TextView) row.findViewById(R.id.color);
            txt_color.setText( "Color = " + Integer.toHexString( color ));
            return(row);
        }
    }
}
