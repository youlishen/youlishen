/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.searchfragment.list;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.legacy.app.FragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.extensions.PhoneDirectoryExtenderAccessor;
import com.android.dialer.R;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.animation.AnimationListenerAdapter;
import com.android.dialer.app.DialtactsActivity;
import com.android.dialer.app.settings.SpeedDialListActivity;
import com.android.dialer.app.settings.SpeedDialUtils;
import com.android.dialer.callcomposer.CallComposerActivity;
import com.android.dialer.callintent.CallInitiationType;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.callintent.CallSpecificAppData;
import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.dialer.common.LogUtil;
import com.android.dialer.common.concurrent.DialerExecutor;
import com.android.dialer.common.concurrent.DialerExecutorComponent;
import com.android.dialer.common.concurrent.ThreadUtil;
import com.android.dialer.constants.ActivityRequestCodes;
import com.android.dialer.dialercontact.DialerContact;
import com.android.dialer.dialpadview.DialpadFragment;
import com.android.dialer.dialpadview.DialpadKeyButton;
import com.android.dialer.dialpadview.DialpadView;
import com.android.dialer.dialpadview.SpecialCharSequenceMgr;
import com.android.dialer.dialpadview.UnicodeDialerKeyListener;
import com.android.dialer.duo.DuoComponent;
import com.android.dialer.enrichedcall.EnrichedCallComponent;
import com.android.dialer.enrichedcall.EnrichedCallManager.CapabilitiesListener;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.precall.PreCall;
import com.android.dialer.searchfragment.common.RowClickListener;
import com.android.dialer.searchfragment.common.SearchCursor;
import com.android.dialer.searchfragment.cp2.SearchContactsCursorLoader;
import com.android.dialer.searchfragment.directories.DirectoriesCursorLoader;
import com.android.dialer.searchfragment.directories.DirectoriesCursorLoader.Directory;
import com.android.dialer.searchfragment.directories.DirectoryContactsCursorLoader;
import com.android.dialer.searchfragment.list.SearchActionViewHolder.Action;
import com.android.dialer.searchfragment.nearbyplaces.NearbyPlacesCursorLoader;
import com.android.dialer.storage.StorageComponent;
import com.android.dialer.telecom.TelecomUtil;
import com.android.dialer.util.CallUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.util.ViewUtil;
import com.android.dialer.widget.EmptyContentView;
import com.android.dialer.widget.EmptyContentView.OnEmptyViewActionButtonClickedListener;
import com.android.wuliu.WuLiuContant;
import com.android.wuliu.WuLiuExecutor;
import com.android.wuliu.WuLiuManager;
import com.android.wuliu.WuLiuOrderInfoBean;
import com.google.common.base.Ascii;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fragment used for searching contacts.
 */
public final class NewSearchFragment extends Fragment
  implements LoaderCallbacks<Cursor>,
  OnEmptyViewActionButtonClickedListener,
  CapabilitiesListener,
  OnTouchListener,
  RowClickListener,
  View.OnClickListener,
  View.OnLongClickListener,
  View.OnKeyListener,
  TextWatcher,
  DialpadKeyButton.OnPressedListener {

  private static final String TAG = "NewSearchFragment";

  // Since some of our queries can generate network requests, we should delay them until the user
  // stops typing to prevent generating too much network traffic.
  private static final int NETWORK_SEARCH_DELAY_MILLIS = 500;
  // To prevent constant capabilities updates refreshing the adapter, we want to add a delay between
  // updates so they are bundled together
  private static final int ENRICHED_CALLING_CAPABILITIES_UPDATED_DELAY = 400;

  private static final String KEY_SHOW_ZERO_SUGGEST = "use_zero_suggest";
  private static final String KEY_LOCATION_PROMPT_DISMISSED = "search_location_prompt_dismissed";

  @VisibleForTesting
  public static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 1;
  @VisibleForTesting
  private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

  private static final int CONTACTS_LOADER_ID = 0;
  private static final int NEARBY_PLACES_LOADER_ID = 1;

  // ID for the loader that loads info about all directories (local & remote).
  private static final int DIRECTORIES_LOADER_ID = 2;

  private static final int DIRECTORY_CONTACTS_LOADER_ID = 3;

  private static final String KEY_QUERY = "key_query";
  private static final String KEY_CALL_INITIATION_TYPE = "key_call_initiation_type";

  private View orderInfoLayout;

  private TextView inputWayHint;
  private EmptyContentView emptyContentView;
  private RecyclerView recyclerView;
  private SearchAdapter adapter;
  private String query;
  // Raw query number from dialpad, which may contain special character such as "+". This is used
  // for actions to add contact or send sms.
  private String rawNumber;
  private CallInitiationType.Type callInitiationType = CallInitiationType.Type.UNKNOWN_INITIATION;
  private boolean directoriesDisabledForTesting;

  // Information about all local & remote directories (including ID, display name, etc, but not
  // the contacts in them).
  private final List<Directory> directories = new ArrayList<>();
  private final Runnable loaderCp2ContactsRunnable =
    () -> getLoaderManager().restartLoader(CONTACTS_LOADER_ID, null, this);
  private final Runnable loadNearbyPlacesRunnable =
    () -> getLoaderManager().restartLoader(NEARBY_PLACES_LOADER_ID, null, this);
  private final Runnable loadDirectoryContactsRunnable =
    () -> getLoaderManager().restartLoader(DIRECTORY_CONTACTS_LOADER_ID, null, this);
  private final Runnable capabilitiesUpdatedRunnable = () -> adapter.notifyDataSetChanged();

  private Runnable updatePositionRunnable;

  private DialpadView dialpadView;
  private ViewGroup bottomView;
  private View narrowView;
  private EditText digits;
  private View delete;
  private DialerExecutor<String> initPhoneNumberFormattingTextWatcherExecutor;
  private String prohibitedPhoneNumberRegexp;
  // determines if we want to playback local DTMF tones.
  private boolean dTMFToneEnabled;
  private final Object toneGeneratorLock = new Object();
  private ToneGenerator toneGenerator;
  private String searchOrderPhoneNumber;

  private static final String EXTRA_SEND_EMPTY_FLASH = "com.android.phone.extra.SEND_EMPTY_FLASH";
  private static final int TONE_LENGTH_MS = 150;
  private static final int TONE_LENGTH_INFINITE = -1;
  /**
   * The DTMF tone volume relative to other sounds in the stream
   */
  private static final int TONE_RELATIVE_VOLUME = 80;
  /**
   * Stream type used to play the DTMF tones off call, and mapped to the volume control keys
   */
  private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;
  private final HashSet<View> pressedDialpadKeys = new HashSet<>(12);
  private static final String EMPTY_NUMBER = "";
  private String lastNumberDialed = EMPTY_NUMBER;
  private boolean wasEmptyBeforeTextChange;
  private boolean digitsFilledByIntent;
  /**
   * Animation that slides in.
   */
  private Animation slideIn;
  /**
   * Animation that slides out.
   */
  private Animation slideOut;
  AnimationListenerAdapter slideInListener =
    new AnimationListenerAdapter() {
      @Override
      public void onAnimationEnd(Animation animation) {
        dialpadView.setVisibility(View.VISIBLE);
        bottomView.setVisibility(View.VISIBLE);
        narrowView.setVisibility(View.GONE);
      }
    };
  /**
   * Listener for after slide out animation completes on dialer fragment.
   */
  AnimationListenerAdapter slideOutListener =
    new AnimationListenerAdapter() {
      @Override
      public void onAnimationEnd(Animation animation) {
        dialpadView.setVisibility(View.GONE);
        bottomView.setVisibility(View.GONE);
        narrowView.setVisibility(View.VISIBLE);
      }
    };

  public static NewSearchFragment newInstance(boolean showZeroSuggest) {
    NewSearchFragment fragment = new NewSearchFragment();
    Bundle args = new Bundle();
    args.putBoolean(KEY_SHOW_ZERO_SUGGEST, showZeroSuggest);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initPhoneNumberFormattingTextWatcherExecutor =
      DialerExecutorComponent.get(getContext())
        .dialerExecutorFactory()
        .createUiTaskBuilder(
          getFragmentManager(),
          "DialpadFragment.initPhoneNumberFormattingTextWatcher",
          new InitPhoneNumberFormattingTextWatcherWorker())
        .onSuccess(watcher -> dialpadView.getDigits().addTextChangedListener(watcher))
        .build();
    prohibitedPhoneNumberRegexp =
      getResources().getString(R.string.config_prohibited_phone_number_regexp);
    slideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.dialpad_slide_in_bottom);
    slideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.dialpad_slide_out_bottom);
    slideIn.setInterpolator(AnimUtils.EASE_IN);
    slideOut.setInterpolator(AnimUtils.EASE_OUT);
    slideIn.setAnimationListener(slideInListener);
    slideOut.setAnimationListener(slideOutListener);
  }

  @Override
  public void onStart() {
    super.onStart();
    synchronized (toneGeneratorLock) {
      if (toneGenerator == null) {
        try {
          toneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
        } catch (Exception e) {
          LogUtil.e(TAG,
            "Exception caught while creating local tone generator: " + e);
          toneGenerator = null;
        }
      }
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    synchronized (toneGeneratorLock) {
      if (toneGenerator != null) {
        toneGenerator.release();
        toneGenerator = null;
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(
    LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_search, parent, false);
    adapter = new SearchAdapter(getContext(), new SearchCursorManager(), this);
    adapter.setQuery(query, rawNumber, callInitiationType);
    adapter.setSearchActions(getActions());
    adapter.setZeroSuggestVisible(getArguments().getBoolean(KEY_SHOW_ZERO_SUGGEST));
    emptyContentView = view.findViewById(R.id.empty_view);
    recyclerView = view.findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setOnTouchListener(this);
    recyclerView.setAdapter(adapter);

    orderInfoLayout= view.findViewById(R.id.wu_liu_order_layout);
    orderInfoLayout.setVisibility(View.GONE);
    searchOrderPhoneNumber = null;

    inputWayHint = view.findViewById(R.id.input_way_hint);
    delete = view.findViewById(R.id.dialpad_floating_action_delete);
    if (delete != null) {
      delete.setOnClickListener(this);
      delete.setOnLongClickListener(this);
    }
    bottomView = view.findViewById(R.id.dialpad_floating_layout);
    narrowView = view.findViewById(R.id.dialpad_floating_action_button_narrow);
    narrowView.setVisibility(View.GONE);
    narrowView.setOnClickListener(this);
    View zoomView = view.findViewById(R.id.dialpad_floating_action_zoom);
    zoomView.setOnClickListener(this);
    View dialView = view.findViewById(R.id.dialpad_floating_action_button);
    dialView.setOnClickListener(this);
    dialpadView = view.findViewById(R.id.dialpad_view);
    dialpadView.setCanDigitsBeEdited(true);
    dialpadView.hideDialEditLayout();
    digits = dialpadView.getDigits();
    digits.setKeyListener(UnicodeDialerKeyListener.INSTANCE);
    digits.setOnClickListener(this);
    digits.setOnKeyListener(this);
    digits.setOnLongClickListener(this);
    digits.addTextChangedListener(this);
    digits.setElegantTextHeight(false);
    View oneButton = view.findViewById(R.id.one);
    if (oneButton != null) {
      configureKeypadListeners(view);
    }
    if (!PermissionsUtil.hasContactsReadPermissions(getContext())) {
      emptyContentView.setDescription(R.string.new_permission_no_search);
      emptyContentView.setActionLabel(R.string.permission_single_turn_on);
      emptyContentView.setActionClickedListener(this);
      emptyContentView.setImage(R.drawable.empty_contacts);
      emptyContentView.setVisibility(View.VISIBLE);
    } else {
      initLoaders();
    }

    if (savedInstanceState != null) {
      setQuery(
        savedInstanceState.getString(KEY_QUERY),
        CallInitiationType.Type.forNumber(savedInstanceState.getInt(KEY_CALL_INITIATION_TYPE)));
    }

    if (updatePositionRunnable != null) {
      ViewUtil.doOnPreDraw(view, false, updatePositionRunnable);
    }
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_CALL_INITIATION_TYPE, callInitiationType.getNumber());
    outState.putString(KEY_QUERY, query);
  }

  private void initLoaders() {
    getLoaderManager().initLoader(CONTACTS_LOADER_ID, null, this);
    loadDirectoriesCursor();
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    LogUtil.i(TAG, "onCreateLoader loading cursor: " + id);
    if (id == CONTACTS_LOADER_ID) {
      return new SearchContactsCursorLoader(getContext(), query, isRegularSearch());
    } else if (id == NEARBY_PLACES_LOADER_ID) {
      // Directories represent contact data sources on the device, but since nearby places aren't
      // stored on the device, they don't have a directory ID. We pass the list of all existing IDs
      // so that we can find one that doesn't collide.
      List<Long> directoryIds = new ArrayList<>();
      for (Directory directory : directories) {
        directoryIds.add(directory.getId());
      }
      return new NearbyPlacesCursorLoader(getContext(), query, directoryIds);
    } else if (id == DIRECTORIES_LOADER_ID) {
      return new DirectoriesCursorLoader(getContext());
    } else if (id == DIRECTORY_CONTACTS_LOADER_ID) {
      return new DirectoryContactsCursorLoader(getContext(), query, directories);
    } else {
      throw new IllegalStateException("Invalid loader id: " + id);
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    LogUtil.i(TAG, " onLoadFinished Loader finished: " + loader + "; cursor: " + cursor);
    if (cursor == null) {
      return;
    }
    for (int i = 0; i<cursor.getColumnCount(); i++) {
      LogUtil.d(TAG, "onLoadFinished [" + i + "]: " + cursor.getColumnName(i));
    }
    if (cursor != null
      && !(loader instanceof DirectoriesCursorLoader)
      && !(cursor instanceof SearchCursor)) {
      throw Assert.createIllegalStateFailException("Cursors must implement SearchCursor");
    }

    if (loader instanceof SearchContactsCursorLoader) {
      adapter.setSearchActions(getActions());// 20210517
      adapter.setContactsCursor((SearchCursor) cursor);

    } else if (loader instanceof NearbyPlacesCursorLoader) {
      adapter.setNearbyPlacesCursor((SearchCursor) cursor);

    } else if (loader instanceof DirectoryContactsCursorLoader) {
      adapter.setDirectoryContactsCursor((SearchCursor) cursor);

    } else if (loader instanceof DirectoriesCursorLoader) {
      directories.clear();
      directories.addAll(DirectoriesCursorLoader.toDirectories(cursor));
      loadNearbyPlacesCursor();
      loadDirectoryContactsCursors();

    } else {
      throw new IllegalStateException("Invalid loader: " + loader);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    LogUtil.i(TAG, "onLoaderReset Loader reset: " + loader);
    if (loader instanceof SearchContactsCursorLoader) {
      adapter.setContactsCursor(null);
    } else if (loader instanceof NearbyPlacesCursorLoader) {
      adapter.setNearbyPlacesCursor(null);
    } else if (loader instanceof DirectoryContactsCursorLoader) {
      adapter.setDirectoryContactsCursor(null);
    }
  }

  public void setRawNumber(String rawNumber) {
    this.rawNumber = rawNumber;
  }

  public void setQuery(String query, CallInitiationType.Type callInitiationType) {
    LogUtil.d(TAG, "setQuery query:" + query + "; callInitiationType:" + callInitiationType);
    this.query = query;
    this.callInitiationType = callInitiationType;
    if (TextUtils.isEmpty(query)) {
      if (inputWayHint != null) {
        inputWayHint.setVisibility(View.VISIBLE);
        orderInfoLayout.setVisibility(View.GONE);
      }
      recyclerView.setVisibility(View.GONE);
      searchOrderPhoneNumber = null;
    } else {
      if (inputWayHint != null) {
        inputWayHint.setVisibility(View.GONE);
        orderInfoLayout.setVisibility(View.GONE);
      }
      recyclerView.setVisibility(View.VISIBLE);
      searchOrderPhoneNumber = null;
    }
    updateInputNumber(query);
    if (adapter != null) {
      adapter.setQuery(query, rawNumber, callInitiationType);
      //adapter.setSearchActions(getActions());
      adapter.setZeroSuggestVisible(isRegularSearch());
      loadCp2ContactsCursor();
      loadNearbyPlacesCursor();
      loadDirectoryContactsCursors();
    }
  }

  /**
   * Translate the search fragment and resize it to fit on the screen.
   */
  public void animatePosition(int start, int end, int duration) {
    // Called before the view is ready, prepare a runnable to run in onCreateView
    if (getView() == null) {
      updatePositionRunnable = () -> animatePosition(start, end, 0);
      return;
    }
    boolean slideUp = start > end;
    Interpolator interpolator = slideUp ? AnimUtils.EASE_IN : AnimUtils.EASE_OUT;
    int startHeight = getActivity().findViewById(android.R.id.content).getHeight();
    int endHeight = startHeight - (end - start);
    getView().setTranslationY(start);
    getView()
      .animate()
      .translationY(end)
      .setInterpolator(interpolator)
      .setDuration(duration)
      .setUpdateListener(
        animation -> setHeight(startHeight, endHeight, animation.getAnimatedFraction()));
    updatePositionRunnable = null;
  }

  private void setHeight(int start, int end, float percentage) {
    View view = getView();
    if (view == null) {
      return;
    }

    FrameLayout.LayoutParams params = (LayoutParams) view.getLayoutParams();
    params.height = (int) (start + (end - start) * percentage);
    view.setLayoutParams(params);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    ThreadUtil.getUiThreadHandler().removeCallbacks(loaderCp2ContactsRunnable);
    ThreadUtil.getUiThreadHandler().removeCallbacks(loadNearbyPlacesRunnable);
    ThreadUtil.getUiThreadHandler().removeCallbacks(loadDirectoryContactsRunnable);
    ThreadUtil.getUiThreadHandler().removeCallbacks(capabilitiesUpdatedRunnable);
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == READ_CONTACTS_PERMISSION_REQUEST_CODE) {
      if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
        // Force a refresh of the data since we were missing the permission before this.
        emptyContentView.setVisibility(View.GONE);
        initLoaders();
      }
    } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
      if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
        // Force a refresh of the data since we were missing the permission before this.
        loadNearbyPlacesCursor();
        adapter.hideLocationPermissionRequest();
      }
    }
  }

  @Override
  public void onEmptyViewActionButtonClicked() {
    String[] deniedPermissions =
      PermissionsUtil.getPermissionsCurrentlyDenied(
        getContext(), PermissionsUtil.allContactsGroupPermissionsUsedInDialer);
    if (deniedPermissions.length > 0) {
      LogUtil.i(
        "NewSearchFragment.onEmptyViewActionButtonClicked",
        "Requesting permissions: " + Arrays.toString(deniedPermissions));
      FragmentCompat.requestPermissions(
        this, deniedPermissions, READ_CONTACTS_PERMISSION_REQUEST_CODE);
    }
  }

  /**
   * Loads info about all directories (local & remote).
   */
  private void loadDirectoriesCursor() {
    if (!directoriesDisabledForTesting) {
      getLoaderManager().initLoader(DIRECTORIES_LOADER_ID, null, this);
    }
  }

  /**
   * Loads contacts stored in directories.
   *
   * <p>Should not be called before finishing loading info about all directories (local & remote).
   */
  private void loadDirectoryContactsCursors() {
    if (directoriesDisabledForTesting) {
      return;
    }

    // Cancel existing load if one exists.
    ThreadUtil.getUiThreadHandler().removeCallbacks(loadDirectoryContactsRunnable);
    ThreadUtil.getUiThreadHandler()
      .postDelayed(loadDirectoryContactsRunnable, NETWORK_SEARCH_DELAY_MILLIS);
  }

  private void loadCp2ContactsCursor() {
    // Cancel existing load if one exists.
    ThreadUtil.getUiThreadHandler().removeCallbacks(loaderCp2ContactsRunnable);
    ThreadUtil.getUiThreadHandler()
      .postDelayed(loaderCp2ContactsRunnable, NETWORK_SEARCH_DELAY_MILLIS);
  }

  /**
   * Loads nearby places.
   *
   * <p>Should not be called before finishing loading info about all directories (local and remote).
   */
  private void loadNearbyPlacesCursor() {
    if (!PermissionsUtil.hasLocationPermissions(getContext())
      && !StorageComponent.get(getContext())
      .unencryptedSharedPrefs()
      .getBoolean(KEY_LOCATION_PROMPT_DISMISSED, false)) {
      if (adapter != null && isRegularSearch() && !hasBeenDismissed()) {
        adapter.showLocationPermissionRequest(
          v -> requestLocationPermission(), v -> dismissLocationPermission());
      }
      return;
    }
    // Cancel existing load if one exists.
    ThreadUtil.getUiThreadHandler().removeCallbacks(loadNearbyPlacesRunnable);

    // If nearby places is not enabled, do not try to load them.
    if (!PhoneDirectoryExtenderAccessor.get(getContext()).isEnabled(getContext())) {
      return;
    }
    ThreadUtil.getUiThreadHandler()
      .postDelayed(loadNearbyPlacesRunnable, NETWORK_SEARCH_DELAY_MILLIS);
  }

  private void requestLocationPermission() {
    Assert.checkArgument(
      !PermissionsUtil.hasPermission(getContext(), ACCESS_FINE_LOCATION),
      "attempted to request already granted location permission");
    String[] deniedPermissions =
      PermissionsUtil.getPermissionsCurrentlyDenied(
        getContext(), PermissionsUtil.allLocationGroupPermissionsUsedInDialer);
    requestPermissions(deniedPermissions, LOCATION_PERMISSION_REQUEST_CODE);
  }

  @VisibleForTesting
  public void dismissLocationPermission() {
    PreferenceManager.getDefaultSharedPreferences(getContext())
      .edit()
      .putBoolean(KEY_LOCATION_PROMPT_DISMISSED, true)
      .apply();
    adapter.hideLocationPermissionRequest();
  }

  private boolean hasBeenDismissed() {
    return PreferenceManager.getDefaultSharedPreferences(getContext())
      .getBoolean(KEY_LOCATION_PROMPT_DISMISSED, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    LogUtil.d(TAG, "onResume");
    EnrichedCallComponent.get(getContext())
      .getEnrichedCallManager()
      .registerCapabilitiesListener(this);
    getLoaderManager().restartLoader(CONTACTS_LOADER_ID, null, this);
    // retrieve the DTMF tone play back setting.
    dTMFToneEnabled = Settings.System.getInt(getActivity().getContentResolver(),
      Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
    pressedDialpadKeys.clear();
  }

  @Override
  public void onPause() {
    super.onPause();
    LogUtil.d(TAG, "onPause");
    lastNumberDialed = EMPTY_NUMBER; // Since we are going to query again, free stale number.
    EnrichedCallComponent.get(getContext())
      .getEnrichedCallManager()
      .unregisterCapabilitiesListener(this);
  }

  @Override
  public void onCapabilitiesUpdated() {
    ThreadUtil.getUiThreadHandler().removeCallbacks(capabilitiesUpdatedRunnable);
    ThreadUtil.getUiThreadHandler()
      .postDelayed(capabilitiesUpdatedRunnable, ENRICHED_CALLING_CAPABILITIES_UPDATED_DELAY);
  }

  // Currently, setting up multiple FakeContentProviders doesn't work and results in this fragment
  // being untestable while it can query multiple datasources. This is a temporary fix.
  // TODO(a bug): Remove this method and test this fragment with multiple data sources
  @VisibleForTesting
  public void setDirectoriesDisabled(boolean disabled) {
    directoriesDisabledForTesting = disabled;
  }

  /**
   * Returns a list of search actions to be shown in the search results.
   *
   * <p>List will be empty if query is 1 or 0 characters or the query isn't from the Dialpad. For
   * the list of supported actions, see {@link SearchActionViewHolder.Action}.
   */
  private List<Integer> getActions() {
    boolean isDialableNumber = PhoneNumberUtils.isGlobalPhoneNumber(query);
    boolean nonDialableQueryInRegularSearch = isRegularSearch() && !isDialableNumber;
    if (TextUtils.isEmpty(query) || query.length() == 1 || nonDialableQueryInRegularSearch) {
      return Collections.emptyList();
    }

    List<Integer> actions = new ArrayList<>();
    if (!isRegularSearch()) {
      actions.add(Action.CREATE_NEW_CONTACT);
      actions.add(Action.ADD_TO_CONTACT);
    }

    if (isRegularSearch() && isDialableNumber) {
      actions.add(Action.MAKE_VOICE_CALL);
    }

    actions.add(Action.SEND_SMS);
    if (CallUtil.isVideoEnabled(getContext())) {
      actions.add(Action.MAKE_VILTE_CALL);
    }

    return actions;
  }

  // Returns true if currently in Regular Search (as opposed to Dialpad Search).
  private boolean isRegularSearch() {
    return callInitiationType == CallInitiationType.Type.REGULAR_SEARCH;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    LogUtil.d(TAG, "onTouch view: " + v + "; event: " + event);
    if (event.getAction() == MotionEvent.ACTION_UP) {
      v.performClick();
    }
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      FragmentUtils.getParentUnsafe(this, SearchFragmentListener.class).onSearchListTouch();
    }
    if (v == recyclerView) {
      dialpadView.startAnimation(slideOut);
    }
    return false;
  }

  @Override
  public void sendMessage(String phoneNumber) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber));
    startActivity(intent);
  }

  @Override
  public void placeVoiceCall(String phoneNumber, int ranking) {
    placeCall(phoneNumber, ranking, false, true);
  }

  @Override
  public void placeVideoCall(String phoneNumber, int ranking) {
    placeCall(phoneNumber, ranking, true, false);
  }

  private void placeCall(
    String phoneNumber, int position, boolean isVideoCall, boolean allowAssistedDial) {
    CallSpecificAppData callSpecificAppData =
      CallSpecificAppData.newBuilder()
        .setCallInitiationType(callInitiationType)
        .setPositionOfSelectedSearchResult(position)
        .setCharactersInSearchString(query == null ? 0 : query.length())
        .setAllowAssistedDialing(allowAssistedDial)
        .build();
    PreCall.start(
      getContext(),
      new CallIntentBuilder(phoneNumber, callSpecificAppData)
        .setIsVideoCall(isVideoCall)
        .setAllowAssistedDial(allowAssistedDial));
    FragmentUtils.getParentUnsafe(this, SearchFragmentListener.class).onCallPlacedFromSearch();
  }

  @Override
  public void placeDuoCall(String phoneNumber) {
    Logger.get(getContext())
      .logImpression(DialerImpression.Type.LIGHTBRINGER_VIDEO_REQUESTED_FROM_SEARCH);
    Intent intent = DuoComponent.get(getContext()).getDuo().getIntent(getContext(), phoneNumber);
    getActivity().startActivityForResult(intent, ActivityRequestCodes.DIALTACTS_DUO);
    FragmentUtils.getParentUnsafe(this, SearchFragmentListener.class).onCallPlacedFromSearch();
  }

  @Override
  public void openCallAndShare(DialerContact contact) {
    Intent intent = CallComposerActivity.newIntent(getContext(), contact);
    DialerUtils.startActivityWithErrorToast(getContext(), intent);
  }

  /**
   * Callback to {@link NewSearchFragment}'s parent to be notified of important events.
   */
  public interface SearchFragmentListener {

    /**
     * Called when the list view in {@link NewSearchFragment} is clicked.
     */
    void onSearchListTouch();

    /**
     * Called when a call is placed from the search fragment.
     */
    void onCallPlacedFromSearch();
  }

  @Override
  public void onClick(View view) {
    int resId = view.getId();

    LogUtil.i(TAG, "onClick view: " + view);
    if (resId == R.id.dialpad_floating_action_button) {
      view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
      handleDialButtonPressed();
    } else if (resId == R.id.deleteButton || resId == R.id.dialpad_floating_action_delete) {
      if (isDigitsEmpty()) {
        updateInputNumber("");
      } else {
        keyPressed(KeyEvent.KEYCODE_DEL);
      }
    } else if (resId == R.id.digits) {
      if (!isDigitsEmpty()) {
        digits.setCursorVisible(true);
      }
    } else if (resId == R.id.dialpad_floating_action_button_narrow) {
      dialpadView.startAnimation(slideIn);
    } else if (resId == R.id.dialpad_floating_action_zoom) {
      dialpadView.startAnimation(slideOut);
    } else {
      LogUtil.w(TAG, "onClick Unexpected event from: " + view);
    }
  }

  @Override
  public boolean onLongClick(View view) {
    final Editable digits = this.digits.getText();
    final int id = view.getId();
    LogUtil.i(TAG, "onLongClick view: " + view);
    switch (id) {
      case R.id.deleteButton:
      case R.id.dialpad_floating_action_delete:
        digits.clear();
        updateInputNumber("");
        return true;
      case R.id.one:
        if (isDigitsEmpty() || TextUtils.equals(this.digits.getText(), "1")) {
          // We'll try to initiate voicemail and thus we want to remove irrelevant string.
          removePreviousDigitIfPossible('1');

          List<PhoneAccountHandle> subscriptionAccountHandles =
            TelecomUtil.getSubscriptionPhoneAccounts(getActivity());
          boolean hasUserSelectedDefault =
            subscriptionAccountHandles.contains(
              TelecomUtil.getDefaultOutgoingPhoneAccount(
                getActivity(), PhoneAccount.SCHEME_VOICEMAIL));
          boolean needsAccountDisambiguation =
            subscriptionAccountHandles.size() > 1 && !hasUserSelectedDefault;

          if (needsAccountDisambiguation || isVoicemailAvailable()) {
            // On a multi-SIM phone, if the user has not selected a default
            // subscription, initiate a call to voicemail so they can select an account
            // from the "Call with" dialog.
            callVoicemail();
          } else if (getActivity() != null) {
            // Voicemail is unavailable maybe because Airplane mode is turned on.
            // Check the current status and show the most appropriate error message.
            final boolean isAirplaneModeOn =
              Settings.System.getInt(
                getActivity().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0)
                != 0;
            if (isAirplaneModeOn) {
              DialogFragment dialogFragment =
                DialpadFragment.ErrorDialogFragment.newInstance(R.string.dialog_voicemail_airplane_mode_message);
              dialogFragment.show(getFragmentManager(), "voicemail_request_during_airplane_mode");
            } else {
              DialogFragment dialogFragment =
                DialpadFragment.ErrorDialogFragment.newInstance(R.string.dialog_voicemail_not_ready_message);
              dialogFragment.show(getFragmentManager(), "voicemail_not_ready");
            }
          }
          return true;
        }
        return false;
      case R.id.zero:
        if (pressedDialpadKeys.contains(view)) {
          // If the zero key is currently pressed, then the long press occurred by touch
          // (and not via other means like certain accessibility input methods).
          // Remove the '0' that was input when the key was first pressed.
          removePreviousDigitIfPossible('0');
        }
        keyPressed(KeyEvent.KEYCODE_PLUS);
        stopTone();
        pressedDialpadKeys.remove(view);
        return true;
      case R.id.digits:
        this.digits.setCursorVisible(true);
        return false;
      case R.id.two:
      case R.id.three:
      case R.id.four:
      case R.id.five:
      case R.id.six:
      case R.id.seven:
      case R.id.eight:
      case R.id.nine:

        LogUtil.i(TAG, "onLongClick" + this.digits.length());
        if (this.digits.length() == 1) {
          final boolean isAirplaneModeOn =
            Settings.System.getInt(getActivity().getContentResolver(),
              Settings.System.AIRPLANE_MODE_ON, 0) != 0;
          if (isAirplaneModeOn) {
            DialogFragment dialogFragment = DialpadFragment.ErrorDialogFragment.newInstance(
              com.android.dialer.R.string.dialog_speed_dial_airplane_mode_message);
            dialogFragment.show(getFragmentManager(),
              "speed_dial_request_during_airplane_mode");
          } else {
            callSpeedNumber(id);
          }
          return true;
        }
        return false;
    }
    return false;
  }

  @Override
  public boolean onKey(View view, int keyCode, KeyEvent event) {
    if (view.getId() == R.id.digits) {
      if (keyCode == KeyEvent.KEYCODE_ENTER) {
        if (!WuLiuManager.getInstance().isAutoDialer()) {
          handleDialButtonPressed();
          return true;
        }
        final String billNumber = digits.getText().toString();
        LogUtil.d(TAG, "onKey Bill number: " + billNumber);
        WuLiuExecutor.execute(() -> {
          if (getActivity() == null  || isAdded()) {
            return;
          }
          WuLiuOrderInfoBean infoBean = WuLiuManager.getInstance().syncQueryOrderByOrderNum(billNumber);
          if (infoBean == null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(),
              getActivity().getString(com.android.dialer.R.string.wuliu_get_bill_info_exception,
                ""), Toast.LENGTH_SHORT).show());
          } else if (infoBean.getException() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(),
              getActivity().getString(com.android.dialer.R.string.wuliu_get_bill_info_exception,
                infoBean.getException()),
              Toast.LENGTH_SHORT).show());
          } else {
            getActivity().runOnUiThread(() -> PreCall.start(getActivity(),
              new CallIntentBuilder(infoBean.getPhoneNumber(), CallInitiationType.Type.DIALPAD)));
          }
        });
      }
    }
    return false;
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    wasEmptyBeforeTextChange = TextUtils.isEmpty(s);
  }

  @Override
  public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
    LogUtil.i(TAG, "onTextChanged input = " + input + "start = "
      + start + " before = " + before + " changeCount = " + changeCount);
    if (wasEmptyBeforeTextChange != TextUtils.isEmpty(input)) {
      final Activity activity = getActivity();
      if (activity != null) {
        activity.invalidateOptionsMenu();
        //updateMenuOverflowButton(wasEmptyBeforeTextChange);
      }
    }
    setQuery(input.toString(), CallInitiationType.Type.DIALPAD);
    // DTMF Tones do not need to be played here any longer -
    // the DTMF dialer handles that functionality now.
  }

  @Override
  public void afterTextChanged(Editable input) {
    // When DTMF dialpad buttons are being pressed, we delay SpecialCharSequenceMgr sequence,
    // since some of SpecialCharSequenceMgr's behavior is too abrupt for the "touch-down"
    // behavior.
    if (!digitsFilledByIntent
      && SpecialCharSequenceMgr.handleChars(getActivity(), input.toString(), digits)) {
      // A special sequence was entered, clear the digits
      digits.getText().clear();
    }

    if (isDigitsEmpty()) {
      digitsFilledByIntent = false;
      digits.setCursorVisible(false);
    }

    //if (dialpadQueryListener != null) {
    //  dialpadQueryListener.onDialpadQueryChanged(digits.getText().toString());
    //}

    //updateDeleteButtonEnabledState();
  }

  private void updateDeleteButtonEnabledState() {
    if (getActivity() == null) {
      return;
    }
    final boolean digitsNotEmpty = !isDigitsEmpty();
    delete.setEnabled(digitsNotEmpty);
  }

  private void callSpeedNumber(int id) {
    int number;

    switch (id) {
      case R.id.two:
        number = 2;
        break;
      case R.id.three:
        number = 3;
        break;
      case R.id.four:
        number = 4;
        break;
      case R.id.five:
        number = 5;
        break;
      case R.id.six:
        number = 6;
        break;
      case R.id.seven:
        number = 7;
        break;
      case R.id.eight:
        number = 8;
        break;
      case R.id.nine:
        number = 9;
        break;
      default:
        return;
    }

    String phoneNumber = SpeedDialUtils.getNumber(getActivity(), number);
    if (phoneNumber == null) {
      showNoSpeedNumberDialog(number);
    } else {
      // final DialtactsActivity activity = getActivity() instanceof DialtactsActivity
      //   ? (DialtactsActivity) getActivity() : null;
      final Intent intent =
        new CallIntentBuilder(phoneNumber, CallInitiationType.Type.DIALPAD).build();
      DialerUtils.startActivityWithErrorToast(getActivity(), intent);
      //hideAndClearDialpad(false);
      hideAndClearDialpad();
    }
  }

  private void hideAndClearDialpad() {
    LogUtil.enterBlock("NewSearchFragment.hideAndClearDialpad");
    FragmentUtils.getParentUnsafe(this, DialpadFragment.DialpadListener.class).onCallPlacedFromDialpad();
  }

  public void callVoicemail() {
    PreCall.start(
      getContext(), CallIntentBuilder.forVoicemail(null, CallInitiationType.Type.DIALPAD));
    hideAndClearDialpad();// TODO
  }

  private boolean isVoicemailAvailable() {
    try {
      PhoneAccountHandle defaultUserSelectedAccount =
        TelecomUtil.getDefaultOutgoingPhoneAccount(getActivity(), PhoneAccount.SCHEME_VOICEMAIL);
      if (defaultUserSelectedAccount == null) {
        // In a single-SIM phone, there is no default outgoing phone account selected by
        // the user, so just call TelephonyManager#getVoicemailNumber directly.
        return !TextUtils.isEmpty(getTelephonyManager().getVoiceMailNumber());
      } else {
        return !TextUtils.isEmpty(
          TelecomUtil.getVoicemailNumber(getActivity(), defaultUserSelectedAccount));
      }
    } catch (SecurityException se) {
      // Possibly no READ_PHONE_STATE privilege.
      LogUtil.w(
        "NewSearchFragment.isVoicemailAvailable",
        "SecurityException is thrown. Maybe privilege isn't sufficient.");
    }
    return false;
  }

  private void showNoSpeedNumberDialog(final int number) {
    new AlertDialog.Builder(getActivity())
      .setTitle(com.android.dialer.R.string.speed_dial_unassigned_dialog_title)
      .setMessage(getString(R.string.speed_dial_unassigned_dialog_message, number))
      .setPositiveButton(com.android.dialer.R.string.yes, (dialog, which) -> {
        // go to speed dial setting screen to set speed dial number.
        Intent intent = new Intent(getActivity(), SpeedDialListActivity.class);
        startActivity(intent);
      })
      .setNegativeButton(R.string.no, null)
      .show();
  }

  private boolean isDigitsEmpty() {
    return digits.length() == 0;
  }

  public void clearDialpad() {
    if (digits != null) {
      digits.getText().clear();
    }
  }

  private TelephonyManager getTelephonyManager() {
    return (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
  }

  private void keyPressed(int keyCode) {
    if (getView() == null || getView().getTranslationY() != 0) {
      return;
    }
    switch (keyCode) {
      case KeyEvent.KEYCODE_1:
        playTone(ToneGenerator.TONE_DTMF_1, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_2:
        playTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_3:
        playTone(ToneGenerator.TONE_DTMF_3, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_4:
        playTone(ToneGenerator.TONE_DTMF_4, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_5:
        playTone(ToneGenerator.TONE_DTMF_5, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_6:
        playTone(ToneGenerator.TONE_DTMF_6, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_7:
        playTone(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_8:
        playTone(ToneGenerator.TONE_DTMF_8, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_9:
        playTone(ToneGenerator.TONE_DTMF_9, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_0:
        playTone(ToneGenerator.TONE_DTMF_0, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_POUND:
        playTone(ToneGenerator.TONE_DTMF_P, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_STAR:
        playTone(ToneGenerator.TONE_DTMF_S, TONE_LENGTH_INFINITE);
        break;
      default:
        break;
    }

    getView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
    digits.onKeyDown(keyCode, event);

    // If the cursor is at the end of the text we hide it.
    final int length = digits.length();
    if (length == digits.getSelectionStart() && length == digits.getSelectionEnd()) {
      digits.setCursorVisible(false);
    }
  }

  private void playTone(int tone, int durationMs) {
    // if local tone playback is disabled, just return.
    if (!dTMFToneEnabled) {
      return;
    }

    // Also do nothing if the phone is in silent mode.
    // We need to re-check the ringer mode for *every* playTone()
    // call, rather than keeping a local flag that's updated in
    // onResume(), since it's possible to toggle silent mode without
    // leaving the current activity (via the ENDCALL-longpress menu.)
    AudioManager audioManager =
      (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
    int ringerMode = audioManager.getRingerMode();
    if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
      || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
      return;
    }

    synchronized (toneGeneratorLock) {
      if (toneGenerator == null) {
        LogUtil.w("NewSearchFragment.playTone", "mToneGenerator == null, tone: " + tone);
        return;
      }

      // Start the new tone (will stop any playing tone)
      toneGenerator.startTone(tone, durationMs);
    }
  }

  private void stopTone() {
    // if local tone playback is disabled, just return.
    if (!dTMFToneEnabled) {
      return;
    }
    synchronized (toneGeneratorLock) {
      if (toneGenerator == null) {
        LogUtil.w("NewSearchFragment.stopTone", "mToneGenerator == null");
        return;
      }
      toneGenerator.stopTone();
    }
  }

  private void removePreviousDigitIfPossible(char digit) {
    final int currentPosition = digits.getSelectionStart();
    if (currentPosition > 0 && digit == digits.getText().charAt(currentPosition - 1)) {
      digits.setSelection(currentPosition);
      digits.getText().delete(currentPosition - 1, currentPosition);
    }
  }

  private void configureKeypadListeners(View fragmentView) {
    final int[] buttonIds =
      new int[]{
        R.id.one,
        R.id.two,
        R.id.three,
        R.id.four,
        R.id.five,
        R.id.six,
        R.id.seven,
        R.id.eight,
        R.id.nine,
        R.id.star,
        R.id.zero,
        R.id.pound
      };

    DialpadKeyButton dialpadKey;

    for (int buttonId : buttonIds) {
      dialpadKey = fragmentView.findViewById(buttonId);
      dialpadKey.setOnPressedListener(this);
      dialpadKey.setOnLongClickListener(this);//wanghongjian add longclick listener
    }
    LogUtil.i(TAG, "configureKeypadListeners");

    // Long-pressing one button will initiate Voicemail.
  /*  final DialpadKeyButton one = fragmentView.findViewById(R.id.one);
    one.setOnLongClickListener(this);

    // Long-pressing zero button will enter '+' instead.
    final DialpadKeyButton zero = fragmentView.findViewById(R.id.zero);
    zero.setOnLongClickListener(this);*/
  }

  private void handleDialButtonPressed() {
    LogUtil.i(TAG, "handleDialButtonPressed");
    if (isDigitsEmpty()) { // No number entered.
      // No real call made, so treat it as a click
      PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
      handleDialButtonClickWithEmptyDigits();
    } else {
      final String number = digits.getText().toString();

      // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
      // test equipment.
      // TODO: clean it up.
      if (number != null
        && !TextUtils.isEmpty(prohibitedPhoneNumberRegexp)
        && number.matches(prohibitedPhoneNumberRegexp)) {
        PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
        if (getActivity() != null) {
          DialogFragment dialogFragment =
            DialpadFragment.ErrorDialogFragment.newInstance(R.string.dialog_phone_call_prohibited_message);
          dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
        }
        // Clear the digits just in case.
        clearDialpad();
      } else {
        PreCall.start(getContext(), new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD));
        hideAndClearDialpad();
      }
    }
  }

  @Override
  public void onPressed(View view, boolean pressed) {

    LogUtil.i(TAG, "onPressed" + pressed + "; view: " + view);
    if (pressed) {
      int resId = view.getId();
      if (resId == R.id.one) {
        keyPressed(KeyEvent.KEYCODE_1);
      } else if (resId == R.id.two) {
        keyPressed(KeyEvent.KEYCODE_2);
      } else if (resId == R.id.three) {
        keyPressed(KeyEvent.KEYCODE_3);
      } else if (resId == R.id.four) {
        keyPressed(KeyEvent.KEYCODE_4);
      } else if (resId == R.id.five) {
        keyPressed(KeyEvent.KEYCODE_5);
      } else if (resId == R.id.six) {
        keyPressed(KeyEvent.KEYCODE_6);
      } else if (resId == R.id.seven) {
        keyPressed(KeyEvent.KEYCODE_7);
      } else if (resId == R.id.eight) {
        keyPressed(KeyEvent.KEYCODE_8);
      } else if (resId == R.id.nine) {
        keyPressed(KeyEvent.KEYCODE_9);
      } else if (resId == R.id.zero) {
        keyPressed(KeyEvent.KEYCODE_0);
      } else if (resId == R.id.pound) {
        keyPressed(KeyEvent.KEYCODE_POUND);
      } else if (resId == R.id.star) {
        keyPressed(KeyEvent.KEYCODE_STAR);
      }
      pressedDialpadKeys.add(view);
    } else {
      pressedDialpadKeys.remove(view);
      if (pressedDialpadKeys.isEmpty()) {
        stopTone();
      }
    }
  }

  private void handleDialButtonClickWithEmptyDigits() {
    LogUtil.d(TAG, "handleDialButtonClickWithEmptyDigits searchOrderPhoneNumber=" + searchOrderPhoneNumber);
    if (!TextUtils.isEmpty(searchOrderPhoneNumber)) {
      PreCall.start(getContext(),
        new CallIntentBuilder(searchOrderPhoneNumber, CallInitiationType.Type.DIALPAD));
      hideAndClearDialpad();
      return;
    }
    if (phoneIsCdma() && isPhoneInUse()) {
      // TODO: Move this logic into services/Telephony
      //
      // This is really CDMA specific. On GSM is it possible
      // to be off hook and wanted to add a 3rd party using
      // the redial feature.
      startActivity(newFlashIntent());
    } else {
      if (!TextUtils.isEmpty(lastNumberDialed)) {
        // Dialpad will be filled with last called number,
        // but we don't want to record it as user action
        PerformanceReport.setIgnoreActionOnce(UiAction.Type.TEXT_CHANGE_WITH_INPUT);

        // Recall the last number dialed.
        digits.setText(lastNumberDialed);

        // ...and move the cursor to the end of the digits string,
        // so you'll be able to delete digits using the Delete
        // button (just as if you had typed the number manually.)
        //
        // Note we use mDigits.getText().length() here, not
        // mLastNumberDialed.length(), since the EditText widget now
        // contains a *formatted* version of mLastNumberDialed (due to
        // mTextWatcher) and its length may have changed.
        digits.setSelection(digits.getText().length());
      } else {
        // There's no "last number dialed" or the
        // background query is still running. There's
        // nothing useful for the Dial button to do in
        // this case.  Note: with a soft dial button, this
        // can never happens since the dial button is
        // disabled under these conditons.
        playTone(ToneGenerator.TONE_PROP_NACK);
      }
    }
  }

  private Intent newFlashIntent() {
    Intent intent = new CallIntentBuilder(EMPTY_NUMBER, CallInitiationType.Type.DIALPAD).build();
    intent.putExtra(EXTRA_SEND_EMPTY_FLASH, true);
    return intent;
  }

  private void playTone(int tone) {
    playTone(tone, TONE_LENGTH_MS);
  }

  /**
   * @return true if the phone is "in use", meaning that at least one line is active (ie. off hook
   * or ringing or dialing, or on hold).
   */
  private boolean isPhoneInUse() {
    return getContext() != null
      && TelecomUtil.isInManagedCall(getContext())
      && FragmentUtils.getParentUnsafe(this, DialpadFragment.HostInterface.class).shouldShowDialpadChooser();
  }

  /**
   * @return true if the phone is a CDMA phone type
   */
  private boolean phoneIsCdma() {
    return getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA;
  }

  private void updateInputNumber(String query) {
    if (getActivity() instanceof DialtactsActivity) {
      ((DialtactsActivity)getActivity()).updateInputNumber(query);
    }
  }

  public void setResultContent(Bundle bundle) {
    LogUtil.d(TAG, "setResultContent bundle: " + bundle);
    if (bundle == null) {
      return;
    }
    digits.setText("");
    searchOrderPhoneNumber = bundle.getString(WuLiuContant.KEY_PHONE_NUMBER);
    String name = bundle.getString(WuLiuContant.KEY_NAME);
    String orderNumber = bundle.getString(WuLiuContant.KEY_ORDER_NUMBER);
    String address = bundle.getString(WuLiuContant.KEY_ADDRESS);
    orderInfoLayout.setVisibility(View.VISIBLE);
    TextView addressView = orderInfoLayout.findViewById(R.id.wu_liu_order_address_content);
    addressView.setText(address);
    TextView nameView = orderInfoLayout.findViewById(R.id.wu_liu_order_name_content);
    nameView.setText(name);
    TextView orderNumberView = orderInfoLayout.findViewById(R.id.wu_liu_order_number_content);
    orderNumberView.setText(orderNumber);
    recyclerView.setVisibility(View.GONE);
    inputWayHint.setVisibility(View.GONE);
  }

  private static class InitPhoneNumberFormattingTextWatcherWorker
    implements DialerExecutor.Worker<String, DialerPhoneNumberFormattingTextWatcher> {

    @Nullable
    @Override
    public DialerPhoneNumberFormattingTextWatcher doInBackground(@Nullable String countryCode) {
      return new DialerPhoneNumberFormattingTextWatcher(countryCode);
    }
  }

  public static class DialerPhoneNumberFormattingTextWatcher
    extends PhoneNumberFormattingTextWatcher {
    private static final Pattern AR_DOMESTIC_CALL_MOBILE_NUMBER_PATTERN;

    // This static initialization block builds a pattern for domestic calls to Argentina mobile
    // numbers:
    // (1) Local calls: 15 <local number>
    // (2) Long distance calls: <area code> 15 <local number>
    // See https://en.wikipedia.org/wiki/Telephone_numbers_in_Argentina for detailed explanations.
    static {
      String regex =
        "0?("
          + "  ("
          + "   11|"
          + "   2("
          + "     2("
          + "       02?|"
          + "       [13]|"
          + "       2[13-79]|"
          + "       4[1-6]|"
          + "       5[2457]|"
          + "       6[124-8]|"
          + "       7[1-4]|"
          + "       8[13-6]|"
          + "       9[1267]"
          + "     )|"
          + "     3("
          + "       02?|"
          + "       1[467]|"
          + "       2[03-6]|"
          + "       3[13-8]|"
          + "       [49][2-6]|"
          + "       5[2-8]|"
          + "       [67]"
          + "     )|"
          + "     4("
          + "       7[3-578]|"
          + "       9"
          + "     )|"
          + "     6("
          + "       [0136]|"
          + "       2[24-6]|"
          + "       4[6-8]?|"
          + "       5[15-8]"
          + "     )|"
          + "     80|"
          + "     9("
          + "       0[1-3]|"
          + "       [19]|"
          + "       2\\d|"
          + "       3[1-6]|"
          + "       4[02568]?|"
          + "       5[2-4]|"
          + "       6[2-46]|"
          + "       72?|"
          + "       8[23]?"
          + "     )"
          + "   )|"
          + "   3("
          + "     3("
          + "       2[79]|"
          + "       6|"
          + "       8[2578]"
          + "     )|"
          + "     4("
          + "       0[0-24-9]|"
          + "       [12]|"
          + "       3[5-8]?|"
          + "       4[24-7]|"
          + "       5[4-68]?|"
          + "       6[02-9]|"
          + "       7[126]|"
          + "       8[2379]?|"
          + "       9[1-36-8]"
          + "     )|"
          + "     5("
          + "       1|"
          + "       2[1245]|"
          + "       3[237]?|"
          + "       4[1-46-9]|"
          + "       6[2-4]|"
          + "       7[1-6]|"
          + "       8[2-5]?"
          + "     )|"
          + "     6[24]|"
          + "     7("
          + "       [069]|"
          + "       1[1568]|"
          + "       2[15]|"
          + "       3[145]|"
          + "       4[13]|"
          + "       5[14-8]|"
          + "       7[2-57]|"
          + "       8[126]"
          + "     )|"
          + "     8("
          + "       [01]|"
          + "       2[15-7]|"
          + "       3[2578]?|"
          + "       4[13-6]|"
          + "       5[4-8]?|"
          + "       6[1-357-9]|"
          + "       7[36-8]?|"
          + "       8[5-8]?|"
          + "       9[124]"
          + "     )"
          + "   )"
          + " )?15"
          + ").*";
      AR_DOMESTIC_CALL_MOBILE_NUMBER_PATTERN = Pattern.compile(regex.replaceAll("\\s+", ""));
    }

    private final String countryCode;

    DialerPhoneNumberFormattingTextWatcher(String countryCode) {
      super(countryCode);
      this.countryCode = countryCode;
    }

    @Override
    public synchronized void afterTextChanged(Editable s) {
      // When the country code is NOT "AR", Android telephony's PhoneNumberFormattingTextWatcher can
      // correctly handle the input so we will let it do its job.
      if (!Ascii.toUpperCase(countryCode).equals("AR")) {
        super.afterTextChanged(s);
        return;
      }

      // When the country code is "AR", PhoneNumberFormattingTextWatcher can also format the input
      // correctly if the number is NOT for a domestic call to a mobile phone.
      String rawNumber = getRawNumber(s);
      Matcher matcher = AR_DOMESTIC_CALL_MOBILE_NUMBER_PATTERN.matcher(rawNumber);
      if (!matcher.matches()) {
        super.afterTextChanged(s);
        return;
      }

      // As modifying the input will trigger another call to afterTextChanged(Editable), we must
      // check whether the input's format has already been removed and return if it has
      // been to avoid infinite recursion.
      if (rawNumber.contentEquals(s)) {
        return;
      }

      // If we reach this point, the country code must be "AR" and variable "s" represents a number
      // for a domestic call to a mobile phone. "s" is incorrectly formatted by Android telephony's
      // PhoneNumberFormattingTextWatcher so we remove its format by replacing it with the raw
      // number.
      s.replace(0, s.length(), rawNumber);

      // Make sure the cursor is at the end of the text.
      Selection.setSelection(s, s.length());

      PhoneNumberUtils.addTtsSpan(s, 0 /* start */, s.length() /* endExclusive */);
    }

    private static String getRawNumber(Editable s) {
      StringBuilder rawNumberBuilder = new StringBuilder();

      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (PhoneNumberUtils.isNonSeparator(c)) {
          rawNumberBuilder.append(c);
        }
      }

      return rawNumberBuilder.toString();
    }
  }
}
