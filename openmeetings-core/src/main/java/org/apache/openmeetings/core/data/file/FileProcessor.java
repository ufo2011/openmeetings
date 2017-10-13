/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.core.data.file;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.openmeetings.util.OmFileHelper.getFileExt;
import static org.apache.openmeetings.util.OpenmeetingsVariables.getWebAppRootKey;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.openmeetings.core.converter.DocumentConverter;
import org.apache.openmeetings.core.converter.FlvExplorerConverter;
import org.apache.openmeetings.core.converter.ImageConverter;
import org.apache.openmeetings.db.dao.file.FileItemDao;
import org.apache.openmeetings.db.entity.file.BaseFileItem.Type;
import org.apache.openmeetings.db.entity.file.FileItem;
import org.apache.openmeetings.util.StoredFile;
import org.apache.openmeetings.util.process.ProcessResult;
import org.apache.openmeetings.util.process.ProcessResultList;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileProcessor {
	private static final Logger log = Red5LoggerFactory.getLogger(FileProcessor.class, getWebAppRootKey());

	//Spring loaded Beans
	@Autowired
	private FlvExplorerConverter flvExplorerConverter;
	@Autowired
	private FileItemDao fileDao;
	@Autowired
	private ImageConverter imageConverter;
	@Autowired
	private DocumentConverter docConverter;

	//TODO this method need to be refactored to throw exceptions
	public ProcessResultList processFile(FileItem f, InputStream is) throws Exception {
		ProcessResultList result = new ProcessResultList();
		// Generate a random string to prevent any problems with
		// foreign characters and duplicates
		String hash = UUID.randomUUID().toString();

		File temp = null;
		try {
			temp = File.createTempFile(String.format("upload_%s", hash), ".tmp");
			copyInputStreamToFile(is, temp);

			String ext = getFileExt(f.getName());
			log.debug("file extension: " + ext);
			StoredFile sf = new StoredFile(hash, ext, temp);
			// Check variable to see if this file is a presentation
			// check if this is a a file that can be converted by
			// openoffice-service
			boolean isOffice = sf.isOffice();
			boolean isPdf = sf.isPdf();
			boolean isImage = sf.isImage();
			boolean isChart = sf.isChart();
			boolean isAsIs = sf.isAsIs();
			boolean isVideo = sf.isVideo();

			log.debug("isAsIs: " + isAsIs);

			// add outputfolders for profiles
			// if it is a presenation it will be copied to another place
			if (!(isOffice || isPdf || isImage || isVideo || isAsIs)) {
				result.addItem("wrongType", new ProcessResult("The file type cannot be converted"));
				return result;
			}
			if (isImage) {
				f.setType(Type.Image);
			} else if (isVideo) {
				f.setType(Type.Video);
			} else if (isChart) {
				f.setType(Type.PollChart);
			} else if (isPdf || isOffice) {
				f.setType(Type.Presentation);
			}
			f.setHash(hash);

			File file = f.getFile(ext);
			log.debug("writing file to: " + file);
			if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
				result.addItem("No parent", new ProcessResult("Unable to create parent for file: " + file.getCanonicalPath()));
				return result;
			}

			log.debug("canBeConverted: " + isOffice);
			if (isOffice || isPdf) {
				copyFile(temp, file);
				// convert to pdf, thumbs, swf and xml-description
				result = docConverter.convertPDF(f, sf);
			} else if (isChart) {
				//TODO should be implemented
				log.debug("uploaded chart file");
			} else if (isImage) {
				// convert it to JPG
				log.debug("##### convert it to JPG: ");
				copyFile(temp, file);
				result = imageConverter.convertImage(f, sf);
			} else if (isVideo) {
				copyFile(temp, file);
				List<ProcessResult> returnList = flvExplorerConverter.convertVideo(f, ext);

				int i = 0;
				for (ProcessResult returnMap : returnList) {
					result.addItem("processVideo " + i++, returnMap);
				}
			}
			f = fileDao.update(f);
			log.debug("fileId: " + f.getId());
		} catch (Exception e) {
			log.debug("Error while processing the file", e);
			result.addItem("exception", new ProcessResult("Unexpected exception: " + e.getMessage()));
			throw e;
		} finally {
			if (temp != null && temp.exists() && temp.isFile()) {
				log.debug("Clean up was successful ? {}", temp.delete());
			}
		}
		return result;
	}
}
