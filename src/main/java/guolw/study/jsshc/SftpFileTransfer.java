package guolw.study.jsshc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpFileTransfer {
	private static final Logger LOG = LoggerFactory.getLogger(SftpFileTransfer.class);
	private Session session = null;
	private ChannelSftp ftpChannel;

	public SftpFileTransfer(Session session) {
		super();
		this.session = session;
	}

	/**
	 * 连接sftp服务器
	 * 
	 * @param host
	 *            远程主机ip地址
	 * @param port
	 *            sftp连接端口，null 时为默认端口
	 * @param user
	 *            用户名
	 * @param password
	 *            密码
	 * @return
	 * @throws JSchException
	 */
	public void connect(String host, Integer port, String user, String password) throws JSchException {
		try {
			JSch jsch = new JSch();
			if (session == null) {
				if (port != null) {
					session = jsch.getSession(user, host, port.intValue());
				} else {
					session = jsch.getSession(user, host);
				}
			}
			if (!session.isConnected()) {
				session.setPassword(password);
				// 设置第一次登陆的时候提示，可选值:(ask | yes | no)
				session.setConfig("StrictHostKeyChecking", "no");
				// 30秒连接超时
				session.connect(30000);
			}
			// sftp 是程序内部固化的名称
			ftpChannel = (ChannelSftp) session.openChannel("sftp");
		} catch (JSchException e) {
			throw e;
		}
	}

	public void close() {
		ftpChannel.disconnect();
		session.disconnect();
	}

	/**
	 * sftp上传文件(夹)
	 * 
	 * @param target
	 * @param source
	 * @throws Exception
	 */
	public void upload(String target, String source) throws Exception {
		LOG.info("sftp upload file [directory] : " + target);
		LOG.info("sftp upload file [uploadFile] : " + source);
		File file = new File(source);
		if (file.exists()) {
			try {
				Vector content = ftpChannel.ls(target);
				if (content == null) {
					ftpChannel.mkdir(target);
				}
			} catch (SftpException e) {
				LOG.error(e.getMessage(), e);
				e.printStackTrace();
			}
			// 进入目标路径
			ftpChannel.cd(target);
			if (file.isFile()) {
				InputStream ins = new FileInputStream(file);
				// 中文名称的
				ftpChannel.put(ins, new String(file.getName().getBytes(), "UTF-8"));
			} else {
				File[] files = file.listFiles();
				for (File file2 : files) {
					String dir = file2.getAbsolutePath();
					/*
					 * if (file2.isDirectory()) { String str =
					 * dir.substring(dir.lastIndexOf(file2.separator)); // directory =
					 * FileUtil.normalize(directory + str); }
					 */
					upload(target, dir);
				}
			}
		}
	}

	/**
	 * sftp下载文件（夹）
	 * 
	 * @param targetDir
	 *            下载文件上级目录
	 * @param srcFile
	 *            下载文件完全路径
	 * @param saveFile
	 *            保存文件路径
	 * @throws UnsupportedEncodingException
	 */
	public void download(String targetDir, String srcFile, String saveFile) throws UnsupportedEncodingException {
		Vector conts = null;
		try {
			conts = ftpChannel.ls(srcFile);
		} catch (SftpException e) {
			e.printStackTrace();
			LOG.debug("ChannelSftp sftp罗列文件发生错误", e);
		}
		File file = new File(saveFile);
		if (!file.exists()) {
			file.mkdir();
		}
		// 文件
		if (srcFile.indexOf(".") > -1) {
			try {
				ftpChannel.get(srcFile, saveFile);
			} catch (SftpException e) {
				e.printStackTrace();
				LOG.debug("ChannelSftp sftp下载文件发生错误", e);
			}
		} else {
			// 文件夹(路径)
			for (Iterator iterator = conts.iterator(); iterator.hasNext();) {
				LsEntry obj = (LsEntry) iterator.next();
				String filename = new String(obj.getFilename().getBytes(), "UTF-8");
				if (!(filename.indexOf(".") > -1)) {
					// directory = FileUtil.normalize(directory +
					// System.getProperty("file.separator") + filename);
					srcFile = targetDir;
					// saveFile = FileUtil.normalize(saveFile + System.getProperty("file.separator")
					// + filename);
				} else {
					// 扫描到文件名为".."这样的直接跳过
					String[] arrs = filename.split("\\.");
					if ((arrs.length > 0) && (arrs[0].length() > 0)) {
						// srcFile = FileUtil.normalize(directory + System.getProperty("file.separator")
						// + filename);
					} else {
						continue;
					}
				}
				download(targetDir, srcFile, saveFile);
			}
		}
	}
}
