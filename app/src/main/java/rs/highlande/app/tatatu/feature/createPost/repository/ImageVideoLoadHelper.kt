package rs.highlande.app.tatatu.feature.createPost.repository

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.model.DataList
import rs.highlande.app.tatatu.model.DataListVideo
import java.net.URLConnection

/**
 * Created by Abhin.
 */

open class ImageVideoLoadHelper(val context: Context) : LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var mLoadComplete: DataLoadComplete
    private var mArrayList = ArrayList<DataList>()
    private var mArrayFolderList = ArrayList<String>()

    companion object {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT, MediaStore.MediaColumns.DURATION)
    }

    fun getAllImagesAndVideos(mFragment: Fragment, loadComplete: DataLoadComplete) {
        mLoadComplete = loadComplete
        LoaderManager.getInstance(mFragment).initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        return CursorLoader(context, MediaStore.Files.getContentUri("external"), projection, selection, null, sortOrder)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        if (cursor != null && !cursor.isClosed) {
            var absolutePathOfImage: String?
            mArrayList.clear()
            mArrayFolderList.clear()

            while (cursor.moveToNext()) {
                val columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                absolutePathOfImage = cursor.getString(columnIndexData)
                val fileName = cursor.getString(cursor.getColumnIndex(projection[1]))
                var bucket = cursor.getString(cursor.getColumnIndex(projection[3]))
                val fileSize = cursor.getInt(cursor.getColumnIndex(projection[4]))
                val width = cursor.getInt(cursor.getColumnIndex(projection[5]))
                val height = cursor.getInt(cursor.getColumnIndex(projection[6]))
                val duration = cursor.getInt(cursor.getColumnIndex(projection[7]))

                //set name of 0 folder as 'Internal Storage'
                if (bucket.isNullOrBlank() || bucket.equals("0", true)) {
                    bucket = context.getString(R.string.basic_internal_storage)
                }

                if (!mArrayFolderList.contains(bucket)) {
                    mArrayFolderList.add(bucket)
                }

                if (absolutePathOfImage.isNullOrBlank() || absolutePathOfImage.lastIndexOf(".") < 0) continue

                //detect all gif files and add the all file in list
                if (!absolutePathOfImage.substring(absolutePathOfImage.lastIndexOf(".")).equals(".gif", true)) {
                    mArrayList.add(
                        if (isVideoFormat(absolutePathOfImage))
                            DataListVideo(bucket, absolutePathOfImage, fileName, fileSize, width, height, videoDuration = duration)
                        else
                            DataList(bucket, absolutePathOfImage, fileName, fileSize, width, height))
                }
            }
            cursor.close()
            mLoadComplete.getAllData(mArrayList, mArrayFolderList)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }

    //Check file type video format
    private fun isVideoFormat(imagePath: String): Boolean {
        val extension = getExtension(imagePath)
        val mimeType = if (TextUtils.isEmpty(extension)) URLConnection.guessContentTypeFromName(imagePath)
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType != null && mimeType.startsWith("video")

    }

    //Get file Extension
    private fun getExtension(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        if (!TextUtils.isEmpty(extension)) {
            return extension
        }
        return if (path.contains(".")) {
            path.substring(path.lastIndexOf(".") + 1, path.length)
        } else {
            ""
        }
    }

    interface DataLoadComplete {
        fun getAllData(mDataArrayList: ArrayList<DataList>, mDataArrayFolderList: ArrayList<String>)
    }
}