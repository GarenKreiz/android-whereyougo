package menion.android.whereyougo.gui.dialogs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import locus.api.objects.extra.Location;
import menion.android.whereyougo.Main;
import menion.android.whereyougo.R;
import menion.android.whereyougo.WUI;
import menion.android.whereyougo.gui.extension.DataInfo;
import menion.android.whereyougo.gui.extension.IconedListAdapter;
import menion.android.whereyougo.gui.extension.UtilsGUI;
import menion.android.whereyougo.hardware.location.LocationState;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.Logger;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import cz.matejcik.openwig.formats.CartridgeFile;

public class DialogChooseCartridge extends DialogFragmentEx {

	private static final String TAG = "DialogChooseCartridge";
	
	private Vector<CartridgeFile> cartridgeFiles;
	
	public DialogChooseCartridge() {
		super();
	}
	
	public void setParams(Vector<CartridgeFile> cartridgeFiles) {
		this.cartridgeFiles = cartridgeFiles;
	}
	
	@Override
	public Dialog createDialog(Bundle savedInstanceState) {
		try {
			// sort cartridges
			final Location actLoc = LocationState.getLocation();
			final Location loc1 = new Location(TAG);
			final Location loc2 = new Location(TAG);
			Collections.sort(cartridgeFiles, new Comparator<CartridgeFile>() {
			
				@Override
				public int compare(CartridgeFile object1, CartridgeFile object2) {
					loc1.setLatitude(object1.latitude);
					loc1.setLongitude(object1.longitude);
					loc2.setLatitude(object2.latitude);
					loc2.setLongitude(object2.longitude);
					return (int) (actLoc.distanceTo(loc1) - actLoc.distanceTo(loc2));
				}
			});
				
			// prepare list
			ArrayList<DataInfo> data = new ArrayList<DataInfo>();
			for (int i = 0; i < cartridgeFiles.size(); i++) {
				CartridgeFile file = cartridgeFiles.get(i);
				byte[] iconData = file.getFile(file.iconId);
				Bitmap icon;
				try {
					icon = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
				} catch (Exception e) {
					icon = Images.getImageB(R.drawable.icon_gc_wherigo);
				}
					
				DataInfo di = new DataInfo(file.name, file.type +
						", " + file.author + ", " + file.version, icon);
				di.value01 = file.latitude;
				di.value02 = file.longitude;
				di.setDistAzi(actLoc);
				data.add(di);
			}

			// complete adapter 
			IconedListAdapter adapter = new IconedListAdapter(A.getMain(), data, null);
			adapter.setTextView02Visible(View.VISIBLE, false);
				
			// create listView
			ListView lv = UtilsGUI.createListView(getActivity(), false, data);
			// set click listener
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					itemClicked(position);
				}
			});
			
			// construct dialog
			return new AlertDialog.Builder(getActivity()).
					setTitle(R.string.choose_cartridge).
					setIcon(R.drawable.ic_title_logo).
					setView(lv).
					setNeutralButton(R.string.close, null).
					create();
		} catch (Exception e) {
			Logger.e(TAG, "createDialog()", e);
		}
		return null;
	}
	
	private void itemClicked(int position) {
		try {
			Main.cartridgeFile = cartridgeFiles.get(position);
			Main.selectedFile = Main.cartridgeFile.filename;
			
			if (Main.cartridgeFile.getSavegame().exists()) {
				UtilsGUI.showDialogQuestion(getActivity(),
						R.string.resume_previous_cartridge,
						new DialogInterface.OnClickListener() {
			
					@Override
					public void onClick(DialogInterface dialog, int btn) {
						File file = new File(Main.getSelectedFile().substring(
								0, Main.getSelectedFile().length() - 3) + "gwl");
						FileOutputStream fos = null;
						try {
							if (!file.exists())
								file.createNewFile();
							fos = new FileOutputStream(file);
						} catch (Exception e) {
							Logger.e(TAG, "onResume() - create empty saveGame file", e);
						}
						Main.restoreCartridge(fos);
					}
				}, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int btn) {
						Main.wui.showScreen(WUI.SCREEN_CART_DETAIL, null);
						try {
							Main.getSaveFile().delete();
						} catch (Exception e) {
							Logger.e(TAG, "onCreate() - deleteSyncFile", e);
						}
					}
				});
			} else {
				Main.wui.showScreen(WUI.SCREEN_CART_DETAIL, null);
			}
		} catch (Exception e) {
			Logger.e(TAG, "onCreate()", e);
		}
		dismiss();
	}
}
