package me.mrmaurice.mb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import uk.co.caprica.vlcj.player.base.AudioChannel;
import uk.co.caprica.vlcj.player.base.Marquee;
import uk.co.caprica.vlcj.player.base.MarqueePosition;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.fullscreen.adaptive.AdaptiveFullScreenStrategy;

public class MrBot {

	private static MrBot instance;

	public static void main(String[] args) throws IOException, InterruptedException {
		instance = new MrBot();
		Thread thread = new Thread(() -> {
			try {
				instance.startBot();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		instance.startVideo();
		thread.start();
		instance.playRandom();
	}

	private JFrame frame;
	private EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private Twirk twirk;

	private Poll poll;
	private Movie current;
	private List<Movie> available;

	private boolean playing = true;
	private SplittableRandom rand = new SplittableRandom();

	public MrBot() {
		sync();
	}

	public void close() {
		info("Se ha cerrado la aplicacion!");
		mediaPlayerComponent.release();
		twirk.disconnect();
		System.exit(0);
	}
	
	public void sync() {
		available = new ArrayList<>();
		File root = new File(".");
		root = new File(root, "PlayList");
		for (File folder : root.listFiles()) {
			if (!folder.isDirectory())
				continue;
			available.add(new Movie(folder));
		}
		if(current != null)
			available.remove(current);
	}

	public void playRandom() {
		if (available.isEmpty()) {
			close();
			return;
		}
		Movie selected = available.get(rand.nextInt(available.size()));
		available.remove(selected);
		current = selected;
		playCurrent();
	}

	public void startVideo() {
		frame = new JFrame("Noche de peliculas");
		frame.setBounds(100, 100, 600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());

		mediaPlayerComponent = new EmbeddedMediaPlayerComponent(null, null, new AdaptiveFullScreenStrategy(frame), null,
				null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void playing(MediaPlayer mediaPlayer) {
				Thread channel = new Thread(() -> {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mediaPlayer.audio().setChannel(AudioChannel.STEREO);
				});
				channel.start();
			}

			@Override
			public void finished(MediaPlayer mediaPlayer) {
				SwingUtilities.invokeLater(() -> playFromPoll());
			}

			@Override
			public void error(MediaPlayer mediaPlayer) {
				close();
			}

		};
		contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);

		frame.setContentPane(contentPane);
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		mediaPlayerComponent.mediaPlayer().fullScreen().set(true);
	}

	public void startBot() throws IOException, InterruptedException {
		twirk = new TwirkBuilder("#notmrmaurice211", "MrBot211", "oauth:fewf3yv44qlc5i4zmypoatcq2gpkuw").build();
		twirk.connect();
		twirk.addIrcListener(new TwirkListener() {
			public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
				String cmd = message.getContent();
				if (cmd.startsWith("!stop")) {
					if (!canRun(sender))
						return;
					close();
					return;
				}

				if (cmd.startsWith("!pausa")) {
					if (!canRun(sender))
						return;

					mediaPlayerComponent.mediaPlayer().controls().pause();
					if (playing)
						info("Se ha pausado la pelicula!");
					else
						info("Se ha reanudado la pelicula!");
					playing = !playing;
				}

				if (cmd.startsWith("!atrasar")) {
					if (!canRun(sender))
						return;
					int amount = -5;
					String[] args = cmd.split(" ");
					if (args.length > 1) {
						Integer time = isInt(args[1]);
						amount = time == null ? amount : time;
						amount = amount > 0 ? amount * -1 : amount;
					}
					mediaPlayerComponent.mediaPlayer().controls().skipTime(amount * 1000);
					info("La pelicula se ha atrasado: " + Math.abs(amount) + "s");
				}

				if (cmd.startsWith("!adelantar")) {
					if (!canRun(sender))
						return;
					int amount = 5;
					String[] args = cmd.split(" ");
					if (args.length > 1) {
						Integer time = isInt(args[1]);
						amount = time == null ? amount : time;
						amount = Math.abs(amount);
					}
					mediaPlayerComponent.mediaPlayer().controls().skipTime(amount * 1000);
					info("La pelicula se ha adelantado: " + amount + "s");
				}

				if (cmd.startsWith("!lista")) {
					String votes = poll.getVotes();
					info("Los votos de las peliculas son: " + votes);
					// votes.forEach(instance::info);
				}

				if (cmd.startsWith("!saltar")) {
					if (!canRun(sender))
						return;
					info("Saltando pelicula!");
					playFromPoll();
				}
				
				if (cmd.startsWith("!sync")) {
					if (!canRun(sender))
						return;
					info("Sincronizando!");
					sync();
					poll = new Poll(available);
				}

				if (cmd.startsWith("!votar")) {
					String[] args = cmd.split(" ");
					if (args.length == 1) {
						info(sender.getUserName() + " el uso del comando es: !votar <num>");
						return;
					}
					Integer time = isInt(args[1]);
					if (time == null) {
						info(sender.getUserName() + " " + time + " no es un numero valido!");
						return;
					}

					poll.vote(sender, time);
				}

			}
		});

	}

	private void playFromPoll() {
		Movie next = poll.getWinner();
		if (next == null)
			close();
		else {
			current = next;
			playCurrent();
		}
	}

	private void playCurrent() {
		poll = new Poll(available);
		mediaPlayerComponent.mediaPlayer().media().play(current.getMovie().toString());
		Marquee marquee = Marquee.marquee().text(current.getName()).size(40).colour(Color.WHITE)
				.position(MarqueePosition.BOTTOM_RIGHT).opacity(0.8f).enable();
		marquee.apply(mediaPlayerComponent.mediaPlayer());
	}

	private Integer isInt(String arg) {
		try {
			return Integer.valueOf(arg);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void info(String msg) {
		twirk.channelMessage(msg);
	}

	private boolean canRun(TwitchUser user) {
		if (!user.isMod() && !user.isOwner()) {
			info(user.getDisplayName() + " no eres mod, no puedes usar este comando!");
			return false;
		}
		return true;
	}

	public static MrBot getInstance() {
		return instance;
	}

}
