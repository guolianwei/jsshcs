package guolw.study.jsshc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Hello world! windows <br>
 * linux子系统中的ssh服务设置 <br>
 * vim /etc/ssh/sshd_config <br>
 * PubkeyAuthentication yes<br>
 * PermitRootLogin yes 注释掉：#StrictModes yes<br>
 */
public class App {
	private static final Logger LOG = Logger.getLogger(SftpFileTransfer.class);

	public static void main(String[] args) throws IOException, JSchException {
		String host = "localhost";
		int port = 22;
		String user = "root";
		String password = "1";
		String command = "ls -a /home/lwguo";
		LOG.info(command);
		String res = exeCommand(host, port, user, password, command);
		LOG.info(res);
		SftpFileTransfer sFtp = new SftpFileTransfer(null);
		sFtp.connect(host, port, user, password);
		try {
			sFtp.upload("/home/lwguo", "D:\\hadoopEcoWin\\derby.log");
		} catch (Exception e) {
			e.printStackTrace();
		}
		sFtp.close();

	}

	public static String exeCommand(String host, int port, String user, String password, String command)
			throws JSchException, IOException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(user, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);
		session.connect();
		ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
		InputStream in = channelExec.getInputStream();
		channelExec.setCommand(command);
		channelExec.setErrStream(System.err);
		channelExec.connect();
		String out = IOUtils.toString(in, "UTF-8");

		channelExec.disconnect();
		session.disconnect();

		return out;
	}
}
