package org.eclipse.osgi.technology.featurelauncher.common.decorator.impl;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.featurelauncher.repository.ArtifactRepository;

public class MutableRepositoryList extends AbstractList<ArtifactRepository> implements List<ArtifactRepository> {

	private final List<ArtifactRepository> mutableList;
	
	public MutableRepositoryList() {
		mutableList = new ArrayList<>();
	}
	
	public MutableRepositoryList(List<ArtifactRepository> repositories) {
		mutableList = new ArrayList<>(repositories);
	}
	
	@Override
	public int size() {
		return mutableList.size();
	}

	@Override
	public ArtifactRepository get(int index) {
		return mutableList.get(index);
	}

	@Override
	public void add(int index, ArtifactRepository element) {
		mutableList.add(index, element);
	}
}
