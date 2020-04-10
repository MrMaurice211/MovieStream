package me.mrmaurice.mb;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.gikk.twirk.types.users.TwitchUser;

public class Poll {

	private List<Movie> list = new LinkedList<>();
	private Set<String> votants = new HashSet<>();

	private String format = "#%d %s: %d";

	public Poll(List<Movie> available) {
		available.forEach(list::add);
		Collections.shuffle(list);
		list.forEach(Movie::resetVotes);
	}

	public String getVotes() {
		StringJoiner joiner = new StringJoiner(" - ");
		for (int i = 0; i < list.size(); i++) {
			Movie m = list.get(i);
			joiner.add(String.format(format, i + 1, m.getName(), m.getVotes()));
		}
		return joiner.toString();
	}

	public void vote(TwitchUser user, int num) {

		String name = user.getUserName();

		if (num > list.size()) {
			MrBot.getInstance().info(name + " la opcion " + num + " no existe!");
			return;
		}

		if (votants.contains(name)) {
			MrBot.getInstance().info(name + " solo puedes votar una vez!");
			return;
		}

		Movie mov = list.get(num - 1);
		mov.addVote();
		MrBot.getInstance().info(name + " has votado por " + mov.getName());
		votants.add(name);

	}

	public Movie getWinner() {
		return list.stream().max(Comparator.comparingInt(Movie::getVotes)).orElse(null);
	}

}
