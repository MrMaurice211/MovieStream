package me.mrmaurice.mb;

import java.io.File;

public class Movie {

	private File movie;
	private String name;
	private int votes;

	public Movie(File folder) {
		name = folder.getName();
		movie = new File(folder, name + ".mp4");
		if (!movie.exists())
			movie = new File(folder, name + ".mkv");
	}

	public File getMovie() {
		return movie;
	}

	public String getName() {
		return name;
	}

	public int getVotes() {
		return votes;
	}
	
	public void resetVotes() {
		votes = 0;
	}

	public void addVote() {
		this.votes += 1;
	}

}
