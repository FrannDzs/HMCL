/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2017  huangyuhui <huanghongxun2008@126.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hmcl.util

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Compress the given directory to a zip file.
 *
 * @param src the source directory or a file.
 * @param destZip the location of dest zip file.
 * @param pathNameCallback callback(pathName, isDirectory) returns your modified pathName
 * @throws IOException
 */
@JvmOverloads
@Throws(IOException::class)
fun zip(src: File, destZip: File, pathNameCallback: ((String, Boolean) -> String?)? = null) {
    ZipOutputStream(destZip.outputStream()).use { zos ->
        val basePath: String
        if (src.isDirectory)
            basePath = src.path
        else
        //直接压缩单个文件时，取父目录
            basePath = src.parent
        zipFile(src, basePath, zos, pathNameCallback)
        zos.closeEntry()
    }
}

/**
 * Zip file.
 * @param src source directory to be compressed.
 * @param basePath the file directory to be compressed, if [src] is a file, this is the parent directory of [src]
 * @param zos the [ZipOutputStream] of dest zip file.
 * @param pathNameCallback callback(pathName, isDirectory) returns your modified pathName, null if you dont want this file zipped
 * @throws IOException
 */
@Throws(IOException::class)
private fun zipFile(src: File,
                    basePath: String,
                    zos: ZipOutputStream,
                    pathNameCallback: ((String, Boolean) -> String?)?) {
    val files: Array<File>
    if (src.isDirectory)
        files = src.listFiles() ?: emptyArray()
    else {
        files = arrayOf(src)
    }
    var pathName: String? //存相对路径(相对于待压缩的根目录)
    val buf = ByteArray(1024)
    for (file in files)
        if (file.isDirectory) {
            pathName = file.path.substring(basePath.length + 1) + "/"
            if (pathNameCallback != null)
                pathName = pathNameCallback.invoke(pathName, true)
            if (pathName == null)
                continue
            zos.putNextEntry(ZipEntry(pathName))
            zipFile(file, basePath, zos, pathNameCallback)
        } else {
            pathName = file.path.substring(basePath.length + 1)
            if (pathNameCallback != null)
                pathName = pathNameCallback.invoke(pathName, true)
            if (pathName == null)
                continue
            file.inputStream().use { inputStream ->
                zos.putNextEntry(ZipEntry(pathName))
                inputStream.copyTo(zos, buf)
            }
        }
}

/**
 * Decompress the given zip file to a directory.
 * @param zip the source zip file.
 * @param dest the dest directory.
 * @param callback will be called for every entry in the zip file, returns false if you dont want this file unzipped.
 * @throws IOException
 */
@JvmOverloads
@Throws(IOException::class)
fun unzip(zip: File, dest: File, callback: ((String) -> Boolean)? = null, ignoreExistsFile: Boolean = true) {
    val buf = ByteArray(1024)
    dest.mkdirs()
    ZipInputStream(zip.inputStream()).use { zipFile ->
        if (zip.exists()) {
            var gbkPath: String
            var strtemp: String
            val strPath = dest.absolutePath
            var zipEnt: ZipEntry?
            while (true) {
                zipEnt = zipFile.nextEntry
                if (zipEnt == null)
                    break
                gbkPath = zipEnt.name
                if (callback != null)
                    if (!callback.invoke(gbkPath))
                        continue
                if (zipEnt.isDirectory) {
                    strtemp = strPath + File.separator + gbkPath
                    val dir = File(strtemp)
                    dir.mkdirs()
                } else {
                    //读写文件
                    gbkPath = zipEnt.name
                    strtemp = strPath + File.separator + gbkPath
                    //建目录
                    val strsubdir = gbkPath
                    for (i in 0..strsubdir.length - 1)
                        if (strsubdir.substring(i, i + 1).equals("/", ignoreCase = true)) {
                            val temp = strPath + File.separator + strsubdir.substring(0, i)
                            val subdir = File(temp)
                            if (!subdir.exists())
                                subdir.mkdir()
                        }
                    if (ignoreExistsFile && File(strtemp).exists())
                        continue
                    File(strtemp).outputStream().use({ fos ->
                        zipFile.copyTo(fos, buf)
                    })
                }
            }
        }
    }
}

/**
 * Decompress the subdirectory of given zip file.
 * @param zip the source zip file.
 * @param dest the dest directory.
 * @param subDirectory the subdirectory of the zip file to be decompressed.
 * @param ignoreExistentFile true if skip all existent files.
 * @throws IOException
 */
@JvmOverloads
@Throws(IOException::class)
fun unzipSubDirectory(zip: File, dest: File, subDirectory: String, ignoreExistentFile: Boolean = true) {
    val buf = ByteArray(1024)
    dest.mkdirs()
    ZipInputStream(zip.inputStream()).use { zipFile ->
        if (zip.exists()) {
            var gbkPath: String
            var strtemp: String
            val strPath = dest.absolutePath
            var zipEnt: ZipEntry?
            while (true) {
                zipEnt = zipFile.nextEntry
                if (zipEnt == null)
                    break
                gbkPath = zipEnt.name
                if (!gbkPath.startsWith(subDirectory))
                    continue
                gbkPath = gbkPath.substring(subDirectory.length)
                if (gbkPath.startsWith("/") || gbkPath.startsWith("\\")) gbkPath = gbkPath.substring(1)
                strtemp = strPath + File.separator + gbkPath
                if (zipEnt.isDirectory) {
                    val dir = File(strtemp)
                    dir.mkdirs()
                } else {
                    //建目录
                    val strsubdir = gbkPath
                    for (i in 0..strsubdir.length - 1)
                        if (strsubdir.substring(i, i + 1).equals("/", ignoreCase = true)) {
                            val temp = strPath + File.separator + strsubdir.substring(0, i)
                            val subdir = File(temp)
                            if (!subdir.exists())
                                subdir.mkdir()
                        }
                    if (ignoreExistentFile && File(strtemp).exists())
                        continue
                    File(strtemp).outputStream().use({ fos ->
                        zipFile.copyTo(fos, buf)
                    })
                }
            }
        }
    }
}