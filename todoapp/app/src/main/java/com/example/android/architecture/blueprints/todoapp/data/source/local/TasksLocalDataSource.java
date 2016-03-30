/*
 * Copyright 2016, The Android Open Source Project
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
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksPersistenceContract.TaskEntry;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Concrete implementation of a data source as a db.
 */
public class TasksLocalDataSource implements TasksDataSource {

    private static TasksLocalDataSource INSTANCE;

    private static final String[] TASK_PROJECTION = new String[]{
            TaskEntry.COLUMN_NAME_ENTRY_ID,
            TaskEntry.COLUMN_NAME_TITLE,
            TaskEntry.COLUMN_NAME_DESCRIPTION,
            TaskEntry.COLUMN_NAME_COMPLETED
    };

    private final BriteDatabase mBriteDatabase;

    // Prevent direct instantiation.
    private TasksLocalDataSource(@NonNull Context context) {
        checkNotNull(context);
        TasksDbHelper dbHelper = new TasksDbHelper(context);
        SqlBrite sqlBrite = SqlBrite.create();
        mBriteDatabase = sqlBrite.wrapDatabaseHelper(dbHelper, Schedulers.io());
    }

    public static TasksLocalDataSource getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new TasksLocalDataSource(context);
        }
        return INSTANCE;
    }

    @Override
    public Observable<List<Task>> getTasks() {
        String sql = String.format("SELECT %s from %s", TaskEntry.TABLE_NAME, TextUtils.join(",", TASK_PROJECTION));
        return mBriteDatabase
                .createQuery(TaskEntry.TABLE_NAME, sql)
                .mapToList(new Func1<Cursor, Task>() {
                    @Override
                    public Task call(Cursor cursor) {
                        String itemId = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_ENTRY_ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TITLE));
                        String description =
                                cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_DESCRIPTION));
                        boolean completed =
                                cursor.getInt(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_COMPLETED)) == 1;
                        return new Task(title, description, itemId, completed);
                    }
                })
                .take(1);
    }

    @Override
    public Observable<Task> getTask(@NonNull String taskId) {
        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {taskId};

        String sql = String.format("SELECT %s from %s WHERE %s", TaskEntry.TABLE_NAME, TextUtils.join(",", TASK_PROJECTION), selection);
        return mBriteDatabase
                .createQuery(TaskEntry.TABLE_NAME, sql, selectionArgs)
                .mapToOneOrDefault(new Func1<Cursor, Task>() {
                    @Override
                    public Task call(Cursor cursor) {
                        String itemId = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_ENTRY_ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TITLE));
                        String description =
                                cursor.getString(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_DESCRIPTION));
                        boolean completed =
                                cursor.getInt(cursor.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_COMPLETED)) == 1;
                        return new Task(title, description, itemId, completed);
                    }
                }, null)
                .take(1);
    }

    @Override
    public void saveTask(@NonNull Task task) {
        checkNotNull(task);

        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_ENTRY_ID, task.getId());
        values.put(TaskEntry.COLUMN_NAME_TITLE, task.getTitle());
        values.put(TaskEntry.COLUMN_NAME_DESCRIPTION, task.getDescription());
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, task.isCompleted());

        mBriteDatabase.insert(TaskEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void completeTask(@NonNull Task task) {
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, true);

        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {task.getId()};

        mBriteDatabase.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void completeTask(@NonNull String taskId) {
        // Not required for the local data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    @Override
    public void activateTask(@NonNull Task task) {
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, false);

        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {task.getId()};

        mBriteDatabase.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void activateTask(@NonNull String taskId) {
        // Not required for the local data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    @Override
    public void clearCompletedTasks() {
        String selection = TaskEntry.COLUMN_NAME_COMPLETED + " LIKE ?";
        String[] selectionArgs = {"1"};

        mBriteDatabase.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public void refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllTasks() {
        mBriteDatabase.delete(TaskEntry.TABLE_NAME, "");
    }

    @Override
    public void deleteTask(@NonNull String taskId) {
        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {taskId};

        mBriteDatabase.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
    }
}
