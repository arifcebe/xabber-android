/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.SavedStatus;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.adapter.StatusEditorAdapter;
import com.xabber.android.ui.adapter.StatusModeAdapter;
import com.xabber.android.ui.helper.ManagedListActivity;
import com.xabber.androiddev.R;

public class StatusEditor extends ManagedListActivity implements
        View.OnClickListener, OnItemClickListener {

    private static final String SAVED_TEXT = "com.xabber.android.ui.StatusEditor.SAVED_TEXT";
    private static final String SAVED_MODE = "com.xabber.android.ui.StatusEditor.SAVED_MODE";

    static final public int CONTEXT_MENU_SELECT_STATUS_ID = 10;
    static final public int CONTEXT_MENU_EDIT_STATUS_ID = 11;
    static final public int CONTEXT_MENU_REMOVE_STATUS_ID = 12;

    private String account;
    private Spinner statusModeView;
    private EditText statusTextView;

    private SavedStatus actionWithItem;
    private StatusEditorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        actionWithItem = null;

        setContentView(R.layout.status_editor);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_default));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        account = StatusEditor.getAccount(intent);
        if (account == null)
            setTitle(getString(R.string.status_editor));
        else
            setTitle(getString(R.string.status_editor_for, AccountManager
                    .getInstance().getVerboseName(account)));

        ListView listView = getListView();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.status_editor_header, listView,
                false);
        listView.addHeaderView(header, null, false);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
        adapter = new StatusEditorAdapter(this);
        setListAdapter(adapter);

        statusTextView = (EditText) header.findViewById(R.id.status_text);
        statusModeView = (Spinner) header.findViewById(R.id.status_icon);
        statusModeView.setAdapter(new StatusModeAdapter(this));
        findViewById(R.id.ok).setOnClickListener(this);

        StatusMode statusMode;
        String statusText;
        if (savedInstanceState == null) {
            if (account == null) {
                statusMode = SettingsManager.statusMode();
                statusText = SettingsManager.statusText();
            } else {
                AccountItem accountItem = AccountManager.getInstance()
                        .getAccount(account);
                if (accountItem == null) {
                    Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
                    finish();
                    return;
                }
                statusMode = accountItem.getFactualStatusMode();
                statusText = accountItem.getStatusText();
            }
        } else {
            statusMode = StatusMode.valueOf(savedInstanceState
                    .getString(SAVED_MODE));
            statusText = savedInstanceState.getString(SAVED_TEXT);
        }
        showStatus(statusMode, statusText);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        StatusMode statusMode = (StatusMode) statusModeView.getSelectedItem();
        outState.putString(SAVED_MODE, statusMode.name());
        outState.putString(SAVED_TEXT, statusTextView.getText().toString());
    }

    private void setStatus(StatusMode statusMode, String statusText) {
        AccountManager accountManager = AccountManager.getInstance();
        if (account != null)
            accountManager.setStatus(account, statusMode, statusText);
        else {
            accountManager.setStatus(statusMode, statusText);
        }
    }

    private void showStatus(StatusMode statusMode, String statusText) {
        for (int index = 0; index < statusModeView.getCount(); index++)
            if (statusMode == statusModeView.getAdapter().getItem(index))
                statusModeView.setSelection(index);
        statusTextView.setText(statusText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.onChange();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.status, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_change_status:
                changeStatus();
                return true;

            case R.id.action_delete_status_message:
                AccountManager.getInstance().clearSavedStatuses();
                adapter.onChange();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        actionWithItem = (SavedStatus) getListView().getItemAtPosition(
                info.position);
        if (actionWithItem == null) // Header
            return;
        menu.add(0, CONTEXT_MENU_SELECT_STATUS_ID, 0,
                getResources().getText(R.string.select_status));
        menu.add(0, CONTEXT_MENU_EDIT_STATUS_ID, 0,
                getResources().getText(R.string.edit_status));
        menu.add(0, CONTEXT_MENU_REMOVE_STATUS_ID, 0,
                getResources().getText(R.string.remove_status));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_STATUS_ID:
                setStatus(actionWithItem.getStatusMode(),
                        actionWithItem.getStatusText());
                finish();
                return true;
            case CONTEXT_MENU_EDIT_STATUS_ID:
                showStatus(actionWithItem.getStatusMode(),
                        actionWithItem.getStatusText());
                return true;
            case CONTEXT_MENU_REMOVE_STATUS_ID:
                AccountManager.getInstance().removeSavedStatus(actionWithItem);
                adapter.onChange();
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                changeStatus();
                break;
            default:
                break;
        }
    }

    private void changeStatus() {
        StatusMode statusMode = (StatusMode) statusModeView
                .getSelectedItem();
        String statusText = statusTextView.getText().toString();
        setStatus(statusMode, statusText);
        finish();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        SavedStatus savedStatus = (SavedStatus) parent.getAdapter().getItem(
                position);
        if (savedStatus == null) // Header
            return;
        setStatus(savedStatus.getStatusMode(), savedStatus.getStatusText());
        finish();
    }

    public static Intent createIntent(Context context) {
        return StatusEditor.createIntent(context, null);
    }

    public static Intent createIntent(Context context, String account) {
        return new AccountIntentBuilder(context, StatusEditor.class)
                .setAccount(account).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }
}
