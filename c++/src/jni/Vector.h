#ifndef Vector_H
#define Vector_H

// $Id: Vector.H 5280 2007-06-23 03:10:02Z mburo $

// This is an ORTS file (c) Michael Buro, licensed under the GPL

// vector + index check

#include <vector>
#include <cassert>

template <typename T>
class Vector : public std::vector<T> {

public:

  Vector() { }
  Vector(size_t n) : std::vector<T>(n) { }
  Vector(size_t n, const T& t) : std::vector<T>(n,t) { }

  template <typename InputIterator> Vector(InputIterator first, InputIterator end)
  {
    this->clear();
    for (; first != end; first++)
      push_back(*first);
  }

  inline T& operator[](size_t i) {
    check(i);
    return std::vector<T>::operator[](i);
  }

  inline const T& operator[](size_t i) const {
    check(i);
    return std::vector<T>::operator[](i);
  }

  inline typename std::vector<T>::iterator find(const T &elem)
  {
    for (typename std::vector<T>::iterator i = this->begin(), end = this->end(); i != end; ++i)
      if (*i == elem)
	return i;
    return this->end();
  }

  inline typename std::vector<T>::const_iterator find(const T &elem) const
  {
    for (typename std::vector<T>::const_iterator i = this->begin(), end = this->end(); i != end; ++i)
      if (*i == elem)
	return i;
    return this->end();
  }

private:

#ifdef NDEBUG
  inline void check(size_t) const { }
#else
  inline void check(size_t i) const { assert(i < this->size()); }
#endif
};

#endif
